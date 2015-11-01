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

public class BridgeServlet extends ProxyServlet {

	private static final long serialVersionUID = -7441088044680657919L;

	private String CONTEXT_PATH = "/simpl4";

	@Override
	public void init() throws ServletException {
		super.init();
		ServletConfig config = getServletConfig();
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
		info("rewriteURI.pathInfo:" + pathInfo);
		String basePath = pathInfo;

		String query = request.getQueryString();
		info("rewriteURI.query:" + query);
		info("rewriteURI.basePath:" + basePath);
		URI uri = null;
		if (query != null && query.length() > 0) {
			uri = URI.create(OsgiStarter.simpl4BaseUrl + basePath + "?" + query);
		} else {
			uri = URI.create(OsgiStarter.simpl4BaseUrl + basePath);
		}
		info("rewriteURI.uri:" + uri);
		return uri;
	}

	private static void info(String msg) {
		System.out.println("BridgeServlet:" + msg);
	}

}

