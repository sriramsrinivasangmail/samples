package sample;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class HelloWorld
 */
@WebServlet("/HelloWorld")
public class HelloWorld extends HttpServlet 
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public HelloWorld() 
	{
	}

	/**
	 * @see HttpServlet#service(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException 
	{
		PrintWriter p = response.getWriter();
        p.println("<BODY>");
        p.println("<H2>Hello World ! </H2>");
        p.println("</BODY>");
	}

}
