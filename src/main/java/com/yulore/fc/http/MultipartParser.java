package com.yulore.fc.http;


import org.apache.commons.fileupload.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

// REF: https://stackoverflow.com/questions/13457503/library-and-examples-of-parsing-multipart-form-data-from-inputstream
public class MultipartParser {
    private static class SimpleContext implements UploadContext {
        private final byte[] request;
        private final String contentType;

        private SimpleContext(final byte[] requestBody, final String contentTypeHeader) {
            this.request = requestBody;
            this.contentType = contentTypeHeader;
        }

        @Override
        public long contentLength() {
            return request.length;
        }

        @Override
        public String getCharacterEncoding() {
            // The 'Content-Type' header may look like:
            // multipart/form-data; charset=UTF-8; boundary="xxxx"
            // in which case we can extract the charset, otherwise,
            // just default to UTF-8.
            final ParameterParser parser = new ParameterParser();
            parser.setLowerCaseNames(true);
            final String charset = parser.parse(contentType, ';').get("charset");
            return charset != null ? charset : "UTF-8";
        }

        @Override
        public int getContentLength() {
            return request.length;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(request);
        }
    }

    /**
     * A form field which stores the field or file data completely in
     * memory. Will be limited by the maximum size of
     * a byte array (about 2GB).
     */
    private static class MemoryFileItem implements FileItem {
        private String fieldName;
        private String fileName;
        private String contentType;
        private boolean isFormField;
        private FileItemHeaders headers;
        private final ByteArrayOutputStream os = new ByteArrayOutputStream();

        public MemoryFileItem(final String fieldName, final String contentType, final boolean isFormField, final String fileName) {
            this.fieldName = fieldName;
            this.contentType = contentType;
            this.isFormField = isFormField;
            this.fileName = fileName;
        }

        @Override
        public void delete() {
        }

        /**
         * Not cached, so only call once.
         */
        @Override
        public byte[] get() {
            return os.toByteArray();
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(get());
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return os;
        }

        @Override
        public long getSize() {
            return os.size();
        }

        @Override
        public String getString() {
            return new String(get(), StandardCharsets.UTF_8);
        }

        @Override
        public String getString(final String encoding) throws UnsupportedEncodingException {
            return new String(get(), encoding);
        }

        @Override
        public boolean isFormField() {
            return isFormField;
        }

        @Override
        public boolean isInMemory() {
            return true;
        }

        @Override
        public void setFieldName(String name) {
            this.fieldName = name;
        }

        @Override
        public void setFormField(boolean state) {
            this.isFormField = state;
        }

        @Override
        public void write(final File file) throws Exception {
        }

        @Override
        public FileItemHeaders getHeaders() {
            return headers;
        }

        @Override
        public void setHeaders(FileItemHeaders headers) {
            this.headers = headers;
        }
    }

    private static class MemoryFileItemFactory implements FileItemFactory {
        @Override
        public FileItem createItem(String fieldName, String contentType, boolean isFormField, String fileName) {
            return new MemoryFileItem(fieldName, contentType, isFormField, fileName);
        }
    }

    /**
     * Assumes the request body really is multipart/form-data.
     * Given the binary request body and the Content-Type header value,
     * attempts to parse fields into a map from field name to list
     * of FileItem objects.
     *
     * Everything is stored in memory and an individual item will only be limited
     * by the maximum size of a byte array (about 2GB). It is recommended that the
     * user sets a limit on maximum upload request size. Doing this will obviously
     * differ by environment.
     *
     * Example:
     *   <code>
     *   var fields = MultipartParser.parseRequest(requestBody, contentTypeHeader);
     *   String firstName = fields.get("firstname").get(0).getString();
     *   byte[] profilePic = fields.get("picture").get(0).get();
     *   </code>
     *
     * @param requestBody The binary request body
     * @param contentTypeHeader The string value of the Content-Type header.
     * @return a map, with each entry having one or more values for that named field.
     * @throws FileUploadException
     */
    public static Map<String, List<FileItem>> parseRequest(byte[] requestBody, String contentTypeHeader)
            throws FileUploadException {
        FileUpload fileUpload = new FileUpload(new MemoryFileItemFactory());
        return fileUpload.parseParameterMap(new SimpleContext(requestBody, contentTypeHeader));
    }
}
