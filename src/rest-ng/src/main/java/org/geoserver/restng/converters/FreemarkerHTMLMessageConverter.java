package org.geoserver.restng.converters;

import freemarker.core.ParseException;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.CollectionModel;
import freemarker.ext.beans.MapModel;
import freemarker.template.*;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.PageInfo;
import org.geoserver.rest.format.ReflectiveHTMLFormat;
import org.geoserver.restng.catalog.wrapper.DefaultFreemarkerContextWrapper;
import org.geoserver.restng.catalog.wrapper.FreemarkerContextWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by tbarsballe on 2017-03-20.
 */
public class FreemarkerHTMLMessageConverter extends BaseMessageConverter {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.restng.converters");

    /**
     * Encoding (null for default)
     */
    protected String encoding;

    public FreemarkerHTMLMessageConverter(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public FreemarkerHTMLMessageConverter(ApplicationContext applicationContext, String encoding) {
        this(applicationContext);
        this.encoding = encoding;
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return MediaType.TEXT_HTML.equals(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return MediaType.TEXT_HTML.equals(mediaType)
            && DefaultFreemarkerContextWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_HTML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Writer tmplWriter = null;
        //Find the template - refer to ReflectiveHTMLFormat
        Class<?> clazz;
        Configuration configuration;
        FreemarkerContextWrapper freemarkerContextWrapper = null;
        if (o instanceof FreemarkerContextWrapper) {
            freemarkerContextWrapper = (FreemarkerContextWrapper) o;
            o = freemarkerContextWrapper.getObject();

            clazz = freemarkerContextWrapper.getObjectClass();
            configuration = createConfiguration(freemarkerContextWrapper, clazz);
        } else {
            clazz = o.getClass();
            configuration = createConfiguration(o, clazz);
        }

        final ObjectWrapper wrapper = configuration.getObjectWrapper();
        final PageInfo pageInfo = (PageInfo) RequestContextHolder.getRequestAttributes().getAttribute( PageInfo.KEY, RequestAttributes.SCOPE_REQUEST );
        configuration.setObjectWrapper(obj -> {
            TemplateModel model = wrapper.wrap(obj);

            if ( model instanceof SimpleHash ) {
                SimpleHash hash = (SimpleHash) model;
                if ( hash.get( "page" ) == null ) {
                    if ( pageInfo != null ) {
                        hash.put( "page", pageInfo );
                    }
                }
            }
            return model;
        });

        Template template = null;

        //first try finding a name directly
        String templateName = getTemplateName( o );
        if ( templateName != null ) {
            template = tryLoadTemplate(configuration, templateName);
            if(template == null)
                template = tryLoadTemplate(configuration, templateName + ".ftl");
        }

        //next look up by the resource being requested
        if ( template == null && pageInfo != null ) {
            //could not find a template bound to the class directly, search by the resource
            // being requested
            String pagePath = pageInfo.getPagePath();
            String r = pagePath.substring(pagePath.lastIndexOf('/')+1);
            //trim trailing slash
            if(r.equals("")) {
                pagePath = pagePath.substring(0, pagePath.length() - 1);
                r = pagePath.substring(pagePath.lastIndexOf('/')+1);
            }
            int i = r.lastIndexOf( "." );
            if ( i != -1 ) {
                r = r.substring( 0, i );
            }

            template = tryLoadTemplate(configuration, r + ".ftl");
        }

        //finally try to find by class
        while( template == null && clazz != null ) {

            template = tryLoadTemplate(configuration, clazz.getSimpleName() + ".ftl");
            if (template == null && FreemarkerContextWrapper.class.isAssignableFrom(clazz)) {
                template = tryLoadTemplate(configuration, clazz.getSimpleName().toLowerCase() + ".ftl");
            }
            if(template == null) {
                for (Class<?> interfaze : clazz.getInterfaces()) {
                    template = tryLoadTemplate(configuration, interfaze.getSimpleName() + ".ftl" );
                    if(template != null)
                        break;
                }
            }

            //move up the class hierachy to continue to look for a matching template
            if ( clazz.getSuperclass() == Object.class ) {
                break;
            }
            clazz = clazz.getSuperclass();
        }

        if ( template != null ) {
            templateName = template.getName();
        }
        else {
            //use a fallback
            templateName = "Object.ftl";
        }

        // Write the object to the template - refer to TemplateRepresentation
        try {
            template = configuration.getTemplate(templateName);
            OutputStream outputStream = outputMessage.getBody();


            if (contentType.getCharSet() != null) {
                tmplWriter = new BufferedWriter(new OutputStreamWriter(
                        outputStream, contentType.getCharSet().name()));
            } else {
                tmplWriter = new BufferedWriter(new OutputStreamWriter(
                        outputStream, template.getEncoding()));
            }

            template.process(o, tmplWriter);
            tmplWriter.flush();
        } catch (TemplateException te) {
            throw new IOException("Template processing error "
                    + te.getMessage());
        }
    }

    /**
     * Tries to load a template, will return null if it's not found. If the template exists
     * but it contains syntax errors an exception will be thrown instead
     *
     * @param configuration The template configuration.
     * @param templateName The name of the template to load.
     */
    protected Template tryLoadTemplate(Configuration configuration, String templateName) {
        try {
            return configuration.getTemplate(templateName);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        } catch(IOException io) {
            LOGGER.log(Level.FINE, "Failed to lookup template " + templateName, io);
            return null;
        }
    }

    /**
     * A hook into the template look-up mechanism.
     * <p>
     * This implementation returns null but subclasses may overide to explicitly specify the name
     * of the template to be used.
     * </p>
     * @param data The object being serialized.
     */
    protected String getTemplateName( Object data ) {
        return null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Configuration createConfiguration(Object data, Class<?> clazz) {

        Configuration cfg = new Configuration( );
        cfg.setObjectWrapper(new ObjectToMapWrapper(clazz));
        if (data instanceof FreemarkerContextWrapper) {
            FreemarkerContextWrapper wrapper = (FreemarkerContextWrapper) data;
            cfg.setClassForTemplateLoading(wrapper.getClassForTemplateLoading(), wrapper.getTemplatePrefix());
        } else {
            cfg.setClassForTemplateLoading(ReflectiveHTMLFormat.class, "");
        }
        if (encoding != null) {
            cfg.setDefaultEncoding(encoding);
        }
        return cfg;
    }

    /**
     * Wraps the object being serialized in a {@link SimpleHash} template model.
     * <p>
     * The method {@link #wrapInternal(Map, SimpleHash, Object)} may be overriden to customize
     * the returned model.
     * </p>
     */
    protected class ObjectToMapWrapper<T> extends BeansWrapper {

        /**
         * The class of object being serialized.
         */
        Class<T> clazz;

        public ObjectToMapWrapper( Class<T> clazz ) {
            this.clazz = clazz;
        }

        @SuppressWarnings("unchecked")
        @Override
        public TemplateModel wrap(Object object) throws TemplateModelException {
            if ( object instanceof Collection) {
                Collection<?> c = (Collection<?>) object;
                if (c.isEmpty()) {
                    SimpleHash hash = new SimpleHash();
                    hash.put( "values", new CollectionModel( c, this ) );
                    return hash;
                }
                else {
                    Object o = c.iterator().next();
                    if ( clazz.isAssignableFrom( o.getClass() ) ) {
                        SimpleHash hash = new SimpleHash();
                        hash.put( "values", new CollectionModel( c, this ) );
                        return hash;
                    }
                }
            }

            if ( object != null && clazz.isAssignableFrom( object.getClass() ) ) {
                HashMap<String, Object> map = new HashMap<String, Object>();

                ClassProperties cp = OwsUtils.getClassProperties(clazz);
                for ( String p : cp.properties() ) {
                    if ( "Class".equals( p ) ) continue;
                    Object value = null;
                    try {
                        value = OwsUtils.get(object, p);
                    } catch(Exception e) {
                        LOGGER.log(Level.WARNING, "Could not resolve property " + p + " of bean " + object, e);
                        value = "** Failed to retrieve value of property " + p + ". Error message is: " + e.getMessage() + "**";
                    }
                    if ( value == null ) {
                        value = "null";
                    }

                    map.put( Character.toLowerCase(p.charAt(0)) + p.substring(1), value.toString());

                }

                SimpleHash model = new SimpleHash();
                model.put( "properties", new MapModel(map, this) );
                model.put( "className", clazz.getSimpleName() );

                wrapInternal(map, model, (T) object);
                return model;
            }

            return super.wrap(object);
        }

        /**
         * Template method to customize the returned template model.
         *
         * @param properties A map of properties obtained reflectively from the object being
         * serialized.
         * @param model The resulting template model.
         * @param object The object being serialized.
         */
        protected void wrapInternal(Map<String, Object> properties, SimpleHash model, T object ) {
        }

    }
}
