package edu.example.docxversioncontrol.storage.minio;

import edu.example.docxversioncontrol.storage.filesystem.StorageFileNotFoundException;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class MinioService {
    @Value("${minio.bucket.name}")
    String bucketName;

    final MinioClient minioClient;

    public void saveFile(Resource file) {
        if (isObjectPresent(file.getFilename()))
            throw new IllegalArgumentException("Unable to save same image twice");

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(file.getFilename())
                    .stream(file.getInputStream(), file.getFile().length(), -1)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while uploading file", e);
        }
    }

    public void saveFile(MultipartFile file) {
        if (isObjectPresent(file.getName()))
            throw new IllegalArgumentException("Unable to save same image twice");

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(file.getOriginalFilename())
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());

        } catch (Exception e) {
            throw new RuntimeException("Error occurred while uploading file", e);
        }
    }

    public String getFileURL(String filename) {
        Map<String, String> reqParams = new HashMap<String, String>();
        reqParams.put("response-content-type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(filename)
                            .expiry(2, TimeUnit.HOURS)
                            .extraQueryParams(reqParams)
                            .build());
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while getting files url", e);
        }
    }

    public void deleteImage(String fileName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build());
        } catch (ErrorResponseException e) {
            throw new StorageFileNotFoundException("Attempt to delete an image with a non-existent name " + fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while deleting file", e);
        }
    }

    public void deleteAll() {
        List<DeleteObject> objectsToDelete = new ArrayList<DeleteObject>();
        Iterable<Result<Item>> allObjects =  minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build());

        try {
            for (Result<Item> object : allObjects) {
                objectsToDelete.add(new DeleteObject(object.get().objectName()));
            }

            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucketName)
                            .objects(objectsToDelete)
                            .build());
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                throw new MinioException("Error while deleting object " + error.objectName() + "; " + error.message());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error occurred while deleting files", e);
        }
    }

    /**
     * Проверка, есть ли в bucket файл с таким именем
     *
     * @param objectName имя файла
     * @return true - такой файл уже есть, false - такого файла еще нет
     */
    private boolean isObjectPresent(String objectName) {
        try {
            StatObjectResponse objectStat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            //если получили ошибку ErrorResponseException, то такого файла нет
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error during checking whether an object is present", e);
        }
    }
}
