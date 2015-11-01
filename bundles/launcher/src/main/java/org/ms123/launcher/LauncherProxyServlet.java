package org.ms123.launcher;

import java.net.URI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.proxy.ProxyServlet;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class LauncherProxyServlet extends ProxyServlet {

	private static final long serialVersionUID = -7441088044680657919L;

	private String CONTEXT_PATH = "/simpl4";
	private String baseUrl;

	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig config = getServletConfig();
		String cp = getServletContext().getRealPath("/WEB-INF");
		FelixServletLauncher.basePath = cp.substring(0, cp.length() - 8);
		String baseUrl = "http://127.0.0.1:10000";
		info("init:" + cp);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			FelixServletLauncher.start();
		});
		info("INITEND");
	}

	@Override
	protected HttpClient newHttpClient() {
		HttpClient httpClient = new HttpClient();
		return httpClient;
	}

	@Override
	protected void customizeProxyRequest(Request proxyRequest, HttpServletRequest request) {
	}

	@Override
	protected URI rewriteURI(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		String basePath = pathInfo.substring(CONTEXT_PATH.length());

		String query = request.getQueryString();
		info("rewriteURI.query:" + query);
		info("rewriteURI.basePath:" + basePath);
		URI uri = null;
		if (query != null && query.length() > 0) {
			uri = URI.create(baseUrl + basePath + "?" + query);
		} else {
			uri = URI.create(baseUrl + basePath);
		}
		info("rewriteURI.uri:" + uri);
		return uri;
	}

	private static void info(String msg) {
		System.out.println("LauncherProxyServlet:" + msg);
	}

}

