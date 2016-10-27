package server.services;

import com.healthmarketscience.rmiio.GZIPRemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteInputStreamServer;
import common.FileInfo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.RemoteException;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileServiceImpl implements FileService {
    private String usernameRootFolderName;

    @Override
    public void sendFile(FileInfo fileInfo, RemoteInputStream remoteInputStream) {
        try {
            InputStream inputStream= RemoteInputStreamClient.wrap(remoteInputStream);
            Path target = fileInfo.getPath();

            Files.copy(inputStream, target, REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public RemoteInputStream getFile(FileInfo fileInfo) throws RemoteException, IOException {
        RemoteInputStreamServer remoteInputStreamServer = null;
        String path = fileInfo.getPath().toString();

        try {
            remoteInputStreamServer = new GZIPRemoteInputStream(new BufferedInputStream(
                    new FileInputStream(path)));
            RemoteInputStream result = remoteInputStreamServer.export();
            remoteInputStreamServer = null;
            return result;
        } finally {
            if(remoteInputStreamServer != null) remoteInputStreamServer.close();
        }
    }


    public void setUsernameRootFolderName(String usernameRootFolderName) {
        this.usernameRootFolderName = usernameRootFolderName;
    }
}
