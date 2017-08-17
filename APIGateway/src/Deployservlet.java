
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet("/Deployservlet")
public class Deployservlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	// location to store file uploaded
	private static final String UPLOAD_DIRECTORY = "/var/serverless/Deployment/";

	// upload settings
	private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
	private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
	private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB

	public Deployservlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// checks if the request actually contains upload file

		if (!ServletFileUpload.isMultipartContent(request)) {
			// if not, we stop here
			PrintWriter writer = response.getWriter();
			writer.println("Error: Form must has enctype=multipart/form-data.");
			writer.flush();
			return;
		}
		
		// configures upload settings
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// sets memory threshold - beyond which files are stored in disk
		factory.setSizeThreshold(MEMORY_THRESHOLD);
		// sets temporary location to store files
		factory.setRepository(new File(System.getProperty("java.io.tmpdir")));

		ServletFileUpload upload = new ServletFileUpload(factory);

		// sets maximum size of upload file
		upload.setFileSizeMax(MAX_FILE_SIZE);

		// sets maximum size of request (include file + form data)
		upload.setSizeMax(MAX_REQUEST_SIZE);

		// constructs the directory path to store upload file
		// this path is relative to application's directory
		String uploadPath = UPLOAD_DIRECTORY;

		// creates the directory if it does not exist
		File uploadDir = new File(uploadPath);
		if (!uploadDir.exists()) {
			uploadDir.mkdir();
		}
		String svcName = null;
		@SuppressWarnings("unused")
		String userName = null;
		String fileName = null;
		String filePath = null;
		
		try {
			// parses the request's content to extract file data
			List<FileItem> formItems = upload.parseRequest(request);

			if (formItems != null && formItems.size() > 0) {
				// iterates over form's fields
				for (FileItem item : formItems) {
					// processes only fields that are not form fields
					if (!item.isFormField()) {
						
						fileName = new File(item.getName()).getName();
						filePath = uploadPath + File.separator + fileName;
						File storeFile = new File(filePath);

						// saves the file on disk
						item.write(storeFile);
						System.out.println("\nFile "+fileName+" Uploaded to "+UPLOAD_DIRECTORY+"\n");
						
					}else{
						if(item.getFieldName().equals("userName")){   
				        	userName=item.getString();
				        }
						if(item.getFieldName().equals("svcName")){   
				          svcName=item.getString();
				        }
					}
				}
				//execute deployment script
				String cmd = UPLOAD_DIRECTORY+"../Deploy.sh "+fileName+" "+svcName;
				Process pr = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));                                          
				String s;
				boolean successful = false;
				while ((s = reader.readLine()) != null) {                                
					System.out.println(s);
					if(s.equals("OK")){
						request.setAttribute("message","Uploaded and Deployed successfully");
						request.setAttribute("url","http://localhost:8080/APIGateway/service/"+svcName);
						successful = true;
						System.out.println("\nUploaded and Deployed successfully");
					}
					if(s.equals("FAIL")){
						request.setAttribute("message","error");
						System.out.println("\nError in Deployment");
					}
				}
				reader.close();
				if(successful){					
					cmd = UPLOAD_DIRECTORY+"../rabbitmqadmin -u admin -p admin declare queue name=svcQueue"+svcName+" durable=true";
					pr = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
					reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					while ((s = reader.readLine()) != null)
						System.out.println(s);
					reader.close();
					cmd = UPLOAD_DIRECTORY+"../rabbitmqadmin -u admin -p admin declare binding source='amq.direct' "
							+ "destination_type=\"queue\" destination=svcQueue"+svcName+" routing_key=svcQueue"+svcName;
					pr = Runtime.getRuntime().exec(new String[]{"bash", "-c", cmd});
					reader = new BufferedReader(new InputStreamReader(pr.getInputStream()));
					while ((s = reader.readLine()) != null)
						System.out.println(s);
					reader.close();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			request.setAttribute("message",
					"error: " + ex.getMessage());
		}
		// redirects client to message page
		getServletContext().getRequestDispatcher("/deploy.jsp").forward(request, response);
	}
}
