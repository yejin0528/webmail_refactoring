/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cse.maven_webmail.model;

import com.sun.mail.smtp.SMTPMessage;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

/**
 *
 * @author jongmin
 */
public class SmtpAgent {

    protected String host = null;
    protected String userid = null;
    protected String to = null;
    protected String cc = null;
    protected String subj = null;
    protected String body = null;
    protected String file1 = null;
    protected InputStream fileStream = null;
    protected boolean isReservationMail = false;

    public SmtpAgent(String host, String userid) {
        this.host = host;
        this.userid = userid;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUserid() {
        return userid;
    }

    public void setUserid(String userid) {
        this.userid = userid;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getSubj() {
        return subj;
    }

    public void setSubj(String subj) {
        this.subj = subj;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getFile1() {
        return file1;
    }

    public void setFile1(String file1) {
        this.file1 = file1;
    }

    public InputStream getFileStream() {
        return fileStream;
    }

    public void setFileStream(InputStream fileStream) {
        this.fileStream = fileStream;
    }

    public boolean getIsReservationMail() {
        return isReservationMail;
    }

    public void setIsReservationMail(boolean isReservationMail) {
        this.isReservationMail = isReservationMail;
    }
    
    // LJM 100418 -  현재 로그인한 사용자의 이메일 주소를 반영하도록 수정되어야 함. - test only
    // LJM 100419 - 일반 웹 서버와의 SMTP 동작시 setFrom() 함수 사용 필요함.
    //              없을 경우 메일 전송이 송신주소가 없어서 걸러짐.
    public boolean sendMessage() {
        boolean status = false;

        // 1. property 설정
        Properties props = System.getProperties();
        props.put("mail.smtp.host", this.host);
        System.out.println("SMTP host : " + props.get("mail.smtp.host"));

        // 2. session 가져오기
        Session session = Session.getDefaultInstance(props, null);
        session.setDebug(false);

        try {
            SMTPMessage msg = new SMTPMessage(session);

            // msg.setFrom(new InternetAddress(this.userid + "@" + this.host));
            msg.setFrom(new InternetAddress(this.userid));  // 200102 LJM - 테스트 목적으로 수정
            //msg.setFrom(new InternetAddress("jongmin@deu.ac.kr"));


            // setRecipient() can be called repeatedly if ';' or ',' exists
            if (this.to.indexOf(';') != -1) {
                this.to = this.to.replaceAll(";", ",");
            }
            msg.setRecipients(Message.RecipientType.TO, this.to);  // 200102 LJM - 수정

            if (this.cc.length() > 1) {
                if (this.cc.indexOf(';') != -1) {
                    this.cc = this.cc.replaceAll(";", ",");
                }
                msg.setRecipients(Message.RecipientType.CC, this.cc);
            }

            // msg.setSubject(MimeUtility.encodeText(this.subj, "euc-kr", "B"));
            msg.setSubject(this.subj);
            msg.setHeader("User-Agent", "LJM-WM/0.1");

            // body
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setText(this.body);

            Multipart mp = new MimeMultipart();
            mp.addBodyPart(mbp);

            // 첨부 파일 추가
            if (this.file1 != null && !getIsReservationMail()) {
                MimeBodyPart a1 = new MimeBodyPart();
                DataSource src = new FileDataSource(this.file1);
                a1.setDataHandler(new DataHandler(src));
                int index = this.file1.lastIndexOf('/');
                String fileName = this.file1.substring(index + 1);
                // "B": base64, "Q": quoted-printable
                a1.setFileName(MimeUtility.encodeText(fileName, "UTF-8", "B"));
                mp.addBodyPart(a1);
            }
            // 예약메일일 때
            if(this.file1 != null && getIsReservationMail()) {
                MimeBodyPart a1 = new MimeBodyPart();
                DataSource src = new ByteArrayDataSource(this.fileStream, "application/octet-stream");
                a1.setDataHandler(new DataHandler(src));
                int index = this.file1.lastIndexOf('/');
                String fileName = this.file1.substring(index + 1);
                // "B": base64, "Q": quoted-printable
                a1.setFileName(MimeUtility.encodeText(fileName, "UTF-8", "B"));
                mp.addBodyPart(a1);
            }
            msg.setContent(mp);

            // 메일 전송
            Transport.send(msg);

            // 메일 전송 완료되었으므로 서버에 저장된
            // 첨부 파일 삭제함
            if (this.file1 != null && !getIsReservationMail()) {
                File f = new File(this.file1);
                if (!f.delete()) {
                    System.err.println(this.file1 + " 파일 삭제가 제대로 안 됨.");
                }
            }
            status = true;
        } catch (Exception ex) {
            System.out.println("sendMessage() error: " + ex);
        }
        return status;
    }  // sendMessage()
}
