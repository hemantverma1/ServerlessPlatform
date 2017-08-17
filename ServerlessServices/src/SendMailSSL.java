import java.io.IOException;
import java.util.Date;
import java.util.Properties;    
import javax.mail.*;    
import javax.mail.internet.*;    
public class SendMailSSL{  
	public void send(String from,String password,String to,String sub,String msg){     	
		//Get properties object    
		Properties props = new Properties();    
		props.put("mail.smtp.host", "smtp.gmail.com");    
		props.put("mail.smtp.socketFactory.port", "465");    
		props.put("mail.smtp.socketFactory.class",    
				"javax.net.ssl.SSLSocketFactory");    
		props.put("mail.smtp.auth", "true");    
		props.put("mail.smtp.port", "465");    
		//get Session   
		Session session = Session.getInstance(props,    
				new javax.mail.Authenticator() {    
			protected PasswordAuthentication getPasswordAuthentication() {    
				return new PasswordAuthentication(from,password);  
			}    
		});    
		//compose message    
		try {    
			MimeMessage message = new MimeMessage(session);    
			message.addRecipient(Message.RecipientType.TO,new InternetAddress(to));    
			message.setSubject(sub);    
			message.setText(msg);    
			//send message  
			Transport.send(message);    
			System.out.println("message sent successfully");  
		} catch (MessagingException e) {throw new RuntimeException(e);}    

	}  

	public void EmailWithAttachment(String userName, String password, String toAddress, String attachFiles)
			throws AddressException, MessagingException {
		// sets SMTP server properties
		String host = "smtp.gmail.com";
		String port = "587";
		//String userName = "serverlessplt@gmail.com";
		//String password = "serverless";
		String subject = "New email with attachments";
		String message = "Image attachment";

		Properties properties = new Properties();
		properties.put("mail.smtp.host", host);
		properties.put("mail.smtp.port", port);
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");
		properties.put("mail.user", userName);
		properties.put("mail.password", password);

		// creates a new session with an authenticator
		Authenticator auth = new Authenticator() {
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(userName, password);
			}
		};
		Session session = Session.getInstance(properties, auth);

		// creates a new e-mail message
		Message msg = new MimeMessage(session);

		msg.setFrom(new InternetAddress(userName));
		InternetAddress[] toAddresses = { new InternetAddress(toAddress) };
		msg.setRecipients(Message.RecipientType.TO, toAddresses);
		msg.setSubject(subject);
		msg.setSentDate(new Date());

		// creates message part
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		messageBodyPart.setContent(message, "text/html");

		// creates multi-part
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(messageBodyPart);

		// adds attachments
		if (attachFiles != null ) {

			MimeBodyPart attachPart = new MimeBodyPart();

			try {
				attachPart.attachFile(attachFiles);
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			multipart.addBodyPart(attachPart);

		}

		// sets the multi-part as e-mail's content
		msg.setContent(multipart);

		// sends the e-mail
		Transport.send(msg);

	}
}

/*public class SendMailSSL{    
	/*public static void main(String[] args) {    
     //from,password,to,subject,message  
     Mailer.send("rp95.patel@gmail.com","SKYWALKER","rp95.patel@gmail.com","hello javatpoint","How r u?");
     try {
		Mailer.EmailWithAttachment("rp95.patel@gmail.com", "/home/riddhi/figures/gray/hermione-granger.jpg");
	} catch (AddressException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (MessagingException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
     //change from, password and to  
 }   
} */   
