package net.datasa.Wiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	@Value("${board.upload.path}")
	private String boardUploadPath;
	String uploadPath = "file:///C:/wiki_upload/";
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry){
		registry.addResourceHandler("/upload/**")
				.addResourceLocations(uploadPath);
		registry.addResourceHandler("/board/file/**")
				.addResourceLocations("file:///" + boardUploadPath);
	}
}
