package org.ms123.launcher;

import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Jsoup;

import java.net.URLEncoder;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.util.Callback;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.proxy.ProxyServlet;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class BridgeServlet extends ProxyServlet {

	private static final long serialVersionUID = -7441088044680657919L;
	private flexjson.JSONSerializer m_js = new flexjson.JSONSerializer();
	private String prefix;

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
	protected void onResponseContent(HttpServletRequest request, HttpServletResponse response, Response proxyResponse, byte[] buffer, int offset, int length, Callback callback) {
		try {
			String pathInfo = request.getPathInfo();
			info("proxying downstream: bytes:" + pathInfo);
			if (pathInfo.endsWith("start.html")) {
				String content = new String(buffer, offset, length, "UTF-8");
				Document doc = Jsoup.parse(content, this.prefix);
				setPrefix(doc, "script", "src");
				setPrefix(doc, "link", "href");
				String html = doc.outerHtml();
				buffer = html.getBytes("UTF-8");
				response.setContentLength(buffer.length);
				response.getOutputStream().write(buffer, 0, buffer.length);
			} else {
				response.getOutputStream().write(buffer, offset, length);
			}
			callback.succeeded();
		} catch (Throwable t) {
			callback.failed(t);
		}
	}

	@Override
	protected URI rewriteURI(HttpServletRequest request) {
		String pathInfo = request.getPathInfo();
		String contextPath = request.getContextPath();
		String servletPath = request.getServletPath();
		info("rewriteURI.contextPath:" + contextPath);
		info("rewriteURI.servletPath:" + servletPath);
		info("rewriteURI.pathInfo:" + pathInfo);
		String basePath = pathInfo;
		this.prefix = contextPath + servletPath;

		String query = request.getQueryString();
		info("rewriteURI.query:" + query);
		info("rewriteURI.basePath:" + basePath);

		URI uri = null;
		String sUri = null;
		if (query != null && query.length() > 0) {
			sUri = OsgiStarter.simpl4BaseUrl + basePath + "?" + query + "&__contextPath=" + this.prefix;
		} else {
			sUri = OsgiStarter.simpl4BaseUrl + basePath + "?__contextPath=" + this.prefix;
		}
		uri = URI.create(sUri);
		info("rewriteURI.uri:" + uri);
		return uri;
	}

	private void setPrefix(Document doc, String tag, String attr) {
		Iterator<Element> it = doc.getElementsByTag(tag).iterator();
		while (it.hasNext()) {
			Element e = it.next();
			if (!e.hasAttr(attr))
				continue;
			String val = e.attr(attr);
			if (val != null && val.length() > 0) {
				info("setting:" + this.prefix + val);
				e.attr(attr, this.prefix + val);
			}
		}
	}

	private static void info(String msg) {
		System.out.println("BridgeServlet:" + msg);
	}

}

