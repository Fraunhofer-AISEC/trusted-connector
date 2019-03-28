package de.fhg.aisec.camel.multipart;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.UploadContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static de.fhg.aisec.camel.multipart.MultiPartConstants.MULTIPART_HEADER;
import static de.fhg.aisec.camel.multipart.MultiPartConstants.MULTIPART_PAYLOAD;

public class MultiPartStringParser implements UploadContext {

    final private InputStream multipartInput;
    final private String boundary;
    private String header;
    private InputStream payload;
    private String payloadContentType;

    MultiPartStringParser(InputStream multipartInput) throws FileUploadException, IOException {
        this.multipartInput = multipartInput;
        this.multipartInput.mark(10240);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(multipartInput, StandardCharsets.UTF_8))) {
            String boundaryLine = reader.readLine();
            this.boundary = boundaryLine.substring(2).trim();
            this.multipartInput.reset();
            for (FileItem i : new FileUpload(new DiskFileItemFactory()).parseRequest(this)) {
                String fieldName = i.getFieldName();
                if (MULTIPART_HEADER.equals(fieldName)) {
                    header = i.getString();
                } else if (MULTIPART_PAYLOAD.equals(fieldName)) {
                    payload = i.getInputStream();
                    payloadContentType = i.getContentType();
                } else {
                    throw new IOException("Unknown multipart field name detected: " + fieldName);
                }
            }
        }
    }

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public int getContentLength() {
        return -1;
    }

    @Override
    public String getContentType() {
        return "multipart/form-data, boundary=" + this.boundary;
    }

    @Override
    public InputStream getInputStream() {
        return multipartInput;
    }

    @Override
    public long contentLength() {
        return -1;
    }

    public String getHeader() {
        return header;
    }

    public InputStream getPayload() {
        return payload;
    }

    public String getPayloadContentType() {
        return payloadContentType;
    }

}