package client.controller;

import client.Refresh;
import client.alert.ErrorAlert;
import client.alert.InfoAlert;
import client.alert.SyncAlert;
import com.healthmarketscience.rmiio.GZIPRemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStream;
import com.healthmarketscience.rmiio.RemoteInputStreamClient;
import com.healthmarketscience.rmiio.RemoteInputStreamServer;
import common.FileInfo;
import common.RemoteSession;
import javafx.concurrent.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.rmi.RemoteException;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileTransferController extends Controller {
    private RemoteSession remoteSession;
    private RemoteInputStream remoteInputStream;
    private boolean copying = false;

    public void syncFiles(List<FileInfo> fileInfoList) throws IOException {
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                Refresh.getInstance().refreshAll();
                copying = true;

                for (int i = 0; i < fileInfoList.size(); i++) {
                    FileInfo fileInfo = fileInfoList.get(i);
                    if (!fileInfo.isBackuped() || fileInfo.isModified()) {
                        boolean success = remoteSession.sendFile(fileInfo, sendFile(fileInfo));
                        fileInfo.setBackuped(success);
                        if(this.isCancelled()) break;
                    }
                    this.updateProgress(i + 1, fileInfoList.size());
                }
                Refresh.getInstance().refreshAll();
                return null ;
            }
        };

        SyncAlert syncAlert = new SyncAlert(task);

        setOnSucceededTask(task, syncAlert);
        setOnCancelledTask(task, syncAlert);
        setOnFailedTask(task, syncAlert);

        new Thread(task).start();
    }

    private RemoteInputStream sendFile(FileInfo fileInfo) throws IOException {
        RemoteInputStreamServer server = null;
        String path = fileInfo.getPath().toString();

        try {
            server = new GZIPRemoteInputStream(new BufferedInputStream(
                    new FileInputStream(path)));
            RemoteInputStream result = server.export();
            remoteInputStream = server;
            server = null;
            return result;
        } finally {
            if(server != null) server.close();
        }
    }

    public void retrieveBackup(FileInfo fileInfo) throws IOException {
        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                Refresh.getInstance().refreshAll();
                if (remoteSession.isModified(fileInfo)) {
                    copying = true;
                    getFile(fileInfo, remoteSession.getFile(fileInfo));
                } else this.done();
                this.updateProgress(1,1);
                Refresh.getInstance().refresh(fileInfo);
                return null ;
            }
        };

        SyncAlert syncAlert = new SyncAlert(task);

        setOnSucceededTask(task, syncAlert);
        setOnCancelledTask(task, syncAlert);
        setOnFailedTask(task, syncAlert);

        new Thread(task).start();

    }

    private void setOnSucceededTask(Task<Void> task, SyncAlert syncAlert){
        task.setOnSucceeded(event -> {
            copying = false;
            syncAlert.setDone();
        });
    }

    private void setOnCancelledTask(Task<Void> task, SyncAlert syncAlert) {
        task.setOnCancelled(event -> {
            try {
                remoteInputStream.close(true);
                copying = false;
            } catch (IOException e) {
                e.printStackTrace();
            }});
    }

    private void setOnFailedTask(Task<Void> task, SyncAlert syncAlert) {
        task.setOnFailed(event -> {
            try {
                copying = false;
                syncAlert.close();
                throw task.getException();
            } catch (Throwable throwable) {
                if (throwable instanceof FileNotFoundException) {
                    new InfoAlert("File not found.");
                }
                else if (throwable instanceof RemoteException) {
                    new ErrorAlert("Service unreachable.");
                }
            }
        });
    }

    private void getFile(FileInfo fileInfo, RemoteInputStream remoteInputStream) throws IOException {
        this.remoteInputStream = remoteInputStream;
        InputStream clientInputStream = RemoteInputStreamClient.wrap(remoteInputStream);
        Path target = fileInfo.getPath();

        Files.copy(clientInputStream, target, REPLACE_EXISTING);
        Files.setLastModifiedTime(target, FileTime.fromMillis(remoteSession.getFileInfo(fileInfo).getLastModified().getTime()));
        clientInputStream.close();
    }

    public void checkIfActualFiles(List<FileInfo> fileInfoList) {
        for (FileInfo fileInfo: fileInfoList) {
            checkIfActualFile(fileInfo);
        }
    }

    public void checkIfActualFile(FileInfo fileInfo) {
        try {
            fileInfo.setModified(remoteSession.isModified(fileInfo));
            fileInfo.setBackuped(true);
        } catch (FileNotFoundException e) {
            fileInfo.setBackuped(false);
        } catch (RemoteException e) {
            new ErrorAlert("Service unreachable.");
        }
    }

    public boolean deleteFile(FileInfo fileInfo) throws RemoteException {
        return remoteSession.deleteFile(fileInfo);
    }

    public void setRemoteSession(RemoteSession remoteSession) {
        this.remoteSession = remoteSession;
    }

    public boolean isCopying() {
        return copying;
    }
}
