package com.abe.gg_stats.controller;

import com.abe.gg_stats.service.ImageProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping("/img")
@Slf4j
public class ImageProxyController {

	private final ImageProxyService imageProxyService;

	@Autowired
	public ImageProxyController(ImageProxyService imageProxyService) {
		this.imageProxyService = imageProxyService;
	}

	@GetMapping
	public ResponseEntity<byte[]> proxy(@RequestParam("url") String url) {
		log.trace("Proxy request, url={}", url);
		return imageProxyService.proxyImage(url).responseEntity();
	}

}