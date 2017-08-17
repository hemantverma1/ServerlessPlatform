import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.*;
import java.net.*;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class ImageService extends coreServiceFramework {
	final static String Path = "/var/serverless/tmp/";

	public void download(String address, String localFileName) {
		OutputStream out = null;
		URLConnection conn = null;
		InputStream in = null;
		try {
			URL url = new URL(address);
			String path = Path + localFileName;
			out = new BufferedOutputStream(new FileOutputStream(path));
			conn = url.openConnection();
			in = conn.getInputStream();
			byte[] buffer = new byte[1024];

			int numRead;
			long numWritten = 0;

			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
			System.out.println(localFileName + "\t" + numWritten);
		} 
		catch (Exception exception) { 
			doLogging("Failed in download "+exception.getMessage()+exception.getStackTrace(), "Error");
		} 
		finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} 
			catch (IOException ioe) {
			}
		}
	}

	public void download(String address) {
		int lastSlashIndex = address.lastIndexOf('/');
		if (lastSlashIndex >= 0 &&
				lastSlashIndex < address.length() - 1) {
			download(address, address.substring(lastSlashIndex + 1));
		} 
		else {
			doLogging("Could not figure out local file name for "+address, "ERROR");
		}
	}

	public void blackNWhite(String link, String email) throws Exception{
		try{
			download(link);
			doLogging("Request came for blackNWhite ", "INFO");
			String outPath=null;
			String fileName=null;
			String imageName = null;
			int lastSlashIndex = link.lastIndexOf('/');
			imageName = link.substring(lastSlashIndex + 1);
			String path = Path+imageName;
			BufferedImage src = ImageIO.read(new File(path));
			ColorConvertOp op =new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
			BufferedImage dest = op.filter(src, null);
			fileName = path.substring(path.lastIndexOf("/")+1);
			path = path.substring(0, path.lastIndexOf("/")+1);
			outPath=path+"BNW_"+fileName;
			ImageIO.write(dest, "jpg", new File(outPath));
			sendMail(outPath, email);
		}
		catch(Exception e){
			doLogging("Failed somewhere "+e.getMessage()+e.getStackTrace(), "Error");
		}
		
	}

	public void grayScale(String link, String email) throws Exception {

		download(link);
		doLogging("Request came for grayScale ", "INFO");
		String outPath=null;
		String fileName=null;

		String imageName = null;
		int lastSlashIndex = link.lastIndexOf('/');
		imageName = link.substring(lastSlashIndex + 1);
		String path = Path+imageName;

		BufferedImage img = ImageIO.read(new File(path));
		//get image width and height
		int width = img.getWidth();
		int height = img.getHeight();

		//convert to grayscale
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				int p = img.getRGB(x,y);
				int a = (p>>24)&0xff;
				int r = (p>>16)&0xff;
				int g = (p>>8)&0xff;
				int b = p&0xff;

				//calculate average
				int avg = (r+g+b)/3;

				//replace RGB value with avg
				p = (a<<24) | (avg<<16) | (avg<<8) | avg;

				img.setRGB(x, y, p);
			}
		}
		fileName = path.substring(path.lastIndexOf("/")+1);
		path = path.substring(0, path.lastIndexOf("/")+1);
		outPath=path+"GRAY_"+fileName;
		ImageIO.write(img, "jpg", new File(outPath));	
		sendMail(outPath, email);

	}
	public void resize(String link, String email) throws Exception {

		download(link);
		String outPath=null;
		String fileName=null;

		int IMG_WIDTH = 300;
		int IMG_HEIGHT = 300;

		String imageName = null;
		int lastSlashIndex = link.lastIndexOf('/');
		imageName = link.substring(lastSlashIndex + 1);
		String path = Path+imageName;

		BufferedImage originalImage = ImageIO.read(new File(path));
		int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

		BufferedImage resizedImage = new BufferedImage(IMG_WIDTH, IMG_HEIGHT, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, IMG_WIDTH, IMG_HEIGHT, null);
		g.dispose();

		fileName = path.substring(path.lastIndexOf("/")+1);
		path = path.substring(0, path.lastIndexOf("/")+1);
		outPath=path+"RESIZE_"+fileName;

		ImageIO.write(resizedImage, "jpg", new File(outPath));
		sendMail(outPath, email);
	}

	private void sendMail(String outputPath, String email){
		ArrayList<functionParam> list = new ArrayList<functionParam>();
		list.add(encodeFunctionParams("serverlessplt@gmail.com"));
		list.add(encodeFunctionParams("serverless"));
		list.add(encodeFunctionParams(email));
		list.add(encodeFunctionParams(outputPath));
		callService("SendMailSSL", "EmailWithAttachment", list);
		File f = new File(outputPath);
		f.delete();
	}


	/*public static void main(String... s) throws Exception
	{
		String output = grayScale("https://yt3.ggpht.com/-v0soe-ievYE/AAAAAAAAAAI/AAAAAAAAAAA/OixOH_h84Po/s900-c-k-no-mo-rj-c0xffffff/photo.jpg");
		System.out.println(output);

		try {
			Mailer ml=new Mailer();
			ml.EmailWithAttachment("jainsidd21@gmail.com", output);
		} catch (AddressException e) {
		e.printStackTrace();
		} catch (MessagingException e) {
		e.printStackTrace();
		}
	}*/
}
