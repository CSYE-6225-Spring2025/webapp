package com.example.CloudDemo.DTO;

public class UploadResponse {
    private String file_name;
    private String id;
    private String url;
    private String upload_date;

    public UploadResponse(String file_name, String id, String url, String upload_date) {
        this.file_name = file_name;
        this.id = id;
        this.url = url;
        this.upload_date = upload_date;
    }

    public String getFile_name() {
        return file_name;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getUpload_date() {
        return upload_date;
    }
}

