package io.github.dunwu.springboot;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Zhang Peng
 * @date 2018-12-29
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 自定义消息转换器
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 清除默认 Json 转换器
        converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);

        // 配置 FastJson
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(SerializerFeature.QuoteFieldNames, SerializerFeature.WriteEnumUsingToString,
            SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat,
            SerializerFeature.DisableCircularReferenceDetect);

        // 添加 FastJsonHttpMessageConverter
        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
        fastJsonHttpMessageConverter.setFastJsonConfig(config);
        List<MediaType> fastMediaTypes = new ArrayList<>();
        fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
        fastJsonHttpMessageConverter.setSupportedMediaTypes(fastMediaTypes);
        converters.add(fastJsonHttpMessageConverter);

        // 添加 StringHttpMessageConverter，解决中文乱码问题
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        converters.add(stringHttpMessageConverter);
    }


    /**
     * 自定义异常拦截处理器
     * @param exceptionResolvers
     */
    @Override
    public void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers) {
        exceptionResolvers
            .add((HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) -> {
                ResponseDTO responseDTO = new ResponseDTO();
                if (e instanceof AppException) {
                    responseDTO.setCode(CodeEn.APP_ERROR.code());
                    StringBuilder sb = new StringBuilder(CodeEn.APP_ERROR.message());
                    sb.append("，详细错误原因：");
                    sb.append(e.getMessage());
                    responseDTO.setMessage(sb.toString());
                } else {
                    responseDTO.setCode(CodeEn.OTHER_ERROR.code());
                    StringBuilder sb = new StringBuilder(CodeEn.OTHER_ERROR.message());
                    sb.append("，详细错误原因：");
                    sb.append(e);
                    responseDTO.setMessage(sb.toString());
                }
                responseResponse(response, responseDTO);
                return new ModelAndView();
            });
    }

    private void responseResponse(HttpServletResponse response, ResponseDTO responseDTO) {
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-type", "application/json;charset=UTF-8");
        response.setStatus(200);
        try {
            response.getWriter().write(JSON.toJSONString(responseDTO));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
