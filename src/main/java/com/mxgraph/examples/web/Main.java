package com.mxgraph.examples.web;


public class Main
{

	public static int PORT = 8080;

	public static void main(String[] args) throws Exception
	{
		/*
		Server server = new Server(PORT);

		// Servlets
		Context context = new Context(server, "/", Context.SESSIONS);
		context.addServlet(new ServletHolder(new Config()), "/Config.ashx");
		context.addServlet(new ServletHolder(new Roundtrip()), "/Roundtrip");
		context.addServlet(new ServletHolder(new Share()), "/Share");
		context.addServlet(new ServletHolder(new ServerView()), "/ServerView");
		context.addServlet(new ServletHolder(new Export()), "/Export");
		context.addServlet(new ServletHolder(new NewExport()), "/NewExport");
		context.addServlet(new ServletHolder(new Deploy()), "/Deploy");
		context.addServlet(new ServletHolder(new Link()), "/Link");
		context.addServlet(new ServletHolder(new EmbedImage()), "/EmbedImage");
		
		// Static file handler. How can we use "." base path with /mxgraph context?
		ResourceHandler fileHandler = new ResourceHandler();
		fileHandler.setResourceBase("..");

		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { new RedirectHandler(),
				fileHandler, context, new DefaultHandler() });
		server.setHandler(handlers);

		server.start();
		server.join();
		*/
	}

	/**
	 * Handles some special redirects for the Java server examples.
	 *
	public static class RedirectHandler extends AbstractHandler
	{

		public void handle(String target, HttpServletRequest request,
				HttpServletResponse response, int dispatch) throws IOException,
				ServletException
		{
			if (target.toLowerCase().endsWith(".xml"))
			{
				// Forces the browser to not cache any XML files
				response.setContentType("text/xml;charset=UTF-8");
				response.setHeader("Pragma", "no-cache"); // HTTP 1.0
				response.setHeader("Cache-control",
						"private, no-cache, no-store");
				response.setHeader("Expires", "0");
			}
			else if (target.equalsIgnoreCase("/")
					|| target.equalsIgnoreCase("/index.html"))
			{
				// Gets the file contents for the index.html file
				String filename = Main.class.getResource(
						"/com/mxgraph/examples/web/resources/index.html")
						.getPath();
				response.getWriter().write(mxUtils.readFile(filename));
				response.setStatus(HttpServletResponse.SC_OK);
				//((Request) request).setHandled(true);
			}
		}

	}
	*/

}
