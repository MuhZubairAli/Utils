package pk.gov.pbs.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FileManager {
    private final String TAG = "FileUtils";
    private static final int REQUEST_EXTERNAL_STORAGE_CODE = 20;
    private static final String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    protected Context mContext;

    public FileManager(Context context){
        this.mContext = context;
    }

    public static String[] getPermissionsRequired(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            String[] permissions = new String[PERMISSIONS_REQUIRED.length + 1];
            System.arraycopy(PERMISSIONS_REQUIRED, 0, permissions, 0, PERMISSIONS_REQUIRED.length);
            permissions[PERMISSIONS_REQUIRED.length] = Manifest.permission.MANAGE_EXTERNAL_STORAGE;
            return permissions;
        }
        return PERMISSIONS_REQUIRED;
    }

    /**
     * Checks if the app has permission to write to device storage
     * For API >= 30 it verifies if has permission to manage all files
     *
     * Returns true if has all permissions other wise returns false which indicates
     * that one or more permissions regarding storage are not granted
     */
    public static boolean hasPermissions(Context context) {
        boolean has = true;
        for (String perm : getPermissionsRequired())
            has = has & ActivityCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED;

        return has && (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager());
    }

    /**
     * Requests for all storage related permissions
     * For API >= 30 it opens up the activity to allow current app the permission to manage all files
     */
    public static void requestPermissions(Activity context){
        if (!hasPermissions(context)) {
            ActivityCompat.requestPermissions(
                    context,
                    getPermissionsRequired(),
                    REQUEST_EXTERNAL_STORAGE_CODE
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestForFileManagerPermission(context);
        }
    }

    /**
     * For API >= 30 this method opens screen for allowing current app to be
     * Manage Application for All Files Access Permission
     * This is required for CRUD operations
     */
    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void requestForFileManagerPermission(Context context){
        StaticUtils.getHandler().post(()->{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                Toast.makeText(context, "On API 30 and above permission to manage all files is required, Please enable the option of \'Allow access to manage all files\'.", Toast.LENGTH_SHORT).show();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()){
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }
    }
    /**
     * @param mode :
     *          Context.MODE_APPEND     --> write more to exist file.
     *          Context.MODE_PRIVATE    --> only available within app.
     *          FileProvider with the FLAG_GRANT_READ_URI_PERMISSION    --> share file.
     * */
    public void writeFileInternal(int mode, String fileName, String data) {

        try {

            // 2 lines of these are the same with one line below.
            // Notice:
            //      + Instead using getFilesDir(), you can use getCacheDir() to get directory
            // of cache place in internal storage.
            //      + openFileOutput will create in app private storage, not in cache.

            //File file = new File(context.getFilesDir(), fileName);
            //FileOutputStream fos = new FileOutputStream(file);

            FileOutputStream fos = mContext.openFileOutput(fileName, mode);

            // Instantiate a stream writer.
            OutputStreamWriter out = new OutputStreamWriter(fos);

            // Add data.
            if(mode == Context.MODE_APPEND) {
                out.append(data).append("\n");
            }else {
                out.write(data);
            }

            // Close stream writer.
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String readFileInternal(String fileName) {

        try {

            // 2 lines of these are the same with one line below.
            // Notice:
            //      + Instead using getFilesDir(), you can use getCacheDir() to get directory
            // of cache place in internal storage.
            //      + openFileInput will open a file in app private storage, not in cache.

//            File file = new File(context.getFilesDir(), fileName);
//            FileInputStream fis = new FileInputStream(file);

            FileInputStream fis = mContext.openFileInput(fileName);

            // Instantiate a buffer reader. (Buffer )
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            // Read every lines in file.
            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }

            // Close buffer reader.
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    public boolean deleteFileInternal(String fileName){
        // If the file is saved on internal storage, you can also ask the Context to locate and delete a file by calling deleteFile()
        return mContext.deleteFile(fileName);
    }

    /* ******************************************************************************************** *
     *                                                                                              *
     *  - If your app uses the WRITE_EXTERNAL_STORAGE permission, then it implicitly has permission *
     *  to read the external storage as well.                                                       *
     *  - Must declare READ_EXTERNAL_STORAGE or WRITE_EXTERNAL_STORAGE before manipulate with       *
     *  external storage.                                                                           *
     *  - Handle file in private external storage in low api (below 18), don't require permission.  *
     *  - Don't be confused external storage with SD external card, since internal SD card is       *
     *  considered as external storage. And internal SD card is a default external storage.         *
     *                                                                                              *
     * ******************************************************************************************** */

    // Checks if external storage is available for read and write.
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Checks if external storage is available to at least read.
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public boolean isExternalStorageAvailable(){
        return isExternalStorageWritable() && isExternalStorageReadable();
    }

    /** Write to a public external directory.
     *  @param mode:
     *          Context.MODE_APPEND     --> write more to exist file.
     *          Context.MODE_PRIVATE    --> only available within app.
     *          FileProvider with the FLAG_GRANT_READ_URI_PERMISSION    --> share file.
     *  @param mainDir: representing the appropriate directory on the external storage ( Environment.DIRECTORY_MUSIC, ...)
     *  @param subFolder: usually an app name to distinguish with another app.
     *  @param fileName: ".nomedia" + fileName to hide it from MediaStore scanning.
     */
    public void writeFileExternalPublic(String mainDir, String subFolder, int mode, String fileName, String data){

        if(!isExternalStorageAvailable()){
            Log.e(TAG, "External storage is not available.");
            return;
        }

        // Get the directory for the user's public mainDir directory.
        String directory = getExternalPublicDirectory(mainDir, subFolder);
        File folder = new File(directory);

        // If directory doesn't exist, create it.
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, fileName);

        Log.d(TAG, "File directory: " + file.getAbsolutePath());
        try {
            if (!file.exists()){
                if(!file.createNewFile())
                    throw new IOException("Failed to create new file.");
            }

            FileOutputStream fos;
            if(mode == Context.MODE_APPEND) {
                fos = new FileOutputStream(file, true);
            }else {
                fos = new FileOutputStream(file);
            }

            // Instantiate a stream writer.
            OutputStreamWriter out = new OutputStreamWriter(fos);

            // Add data.
            if(mode == Context.MODE_APPEND) {
                out.append(data + "\n");
            }else {
                out.write(data);
            }

            // Close stream writer.
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getExternalPublicDirectory(String mainDir, String subFolder){
        File root;
        if(mainDir.isEmpty()){
            root = Environment.getExternalStorageDirectory();
        }else {
            root = Environment.getExternalStoragePublicDirectory(mainDir);
        }

        return root + File.separator  + subFolder;
    }

    public String readFileExternalPublic(String mainDir, String subFolder, String fileName) {

        try {

            String directory = getExternalPublicDirectory(mainDir, subFolder);
            File folder = new File(directory);

            File file = new File(folder, fileName);

            // If file doesn't exist.
            if (!file.exists()) {
                Log.e(TAG, "File doesn't exist.");
                return null;
            }

            FileInputStream fis = new FileInputStream(file);

            // Instantiate a buffer reader. (Buffer )
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            // Read every lines in file.
            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }

            // Close buffer reader.
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    public boolean deleteFileExternalPublic(String mainDir, String subFolder, String fileName){

        String directory = getExternalPublicDirectory(mainDir, subFolder);
        File folder = new File(directory);

        File file = new File(folder, fileName);

        // If file doesn't exist.
        if (!file.exists()) {
            Log.e(TAG, "File doesn't exist.");
            return true;
        }
        return file.delete();
    }

    /** Write to a public external directory.
     *  @param mode:
     *          Context.MODE_APPEND     --> write more to exist file.
     *          Context.MODE_PRIVATE    --> only available within app.
     *          FileProvider with the FLAG_GRANT_READ_URI_PERMISSION    --> share file.
     *  @param mainDir: - Representing the appropriate directory on the external storage ( Environment.DIRECTORY_MUSIC, ...)
     *                  - It can be null --> represent that directory as a parent file of private external storage in the app.
     *  @param subFolder: usually an app name to distinguish with another app.
     */
    public void writeFileExternalPrivate(String mainDir, String subFolder, int mode, String fileName, String data){

        // Get the directory for the user's private mainDir directory.
        String directory = mContext.getExternalFilesDir(mainDir) + File.separator  + subFolder;
        File folder = new File(directory);

        // If directory doesn't exist, create it.
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, fileName);

        try {
            FileOutputStream fos;
//            if(mode == Context.MODE_APPEND) {
//                fos = new FileOutputStream(file, true);
//            }else {
//                fos = new FileOutputStream(file);
//            }

            fos = new FileOutputStream(file);

            // Instantiate a stream writer.
            OutputStreamWriter out = new OutputStreamWriter(fos);

            // Add data.
            if(mode == Context.MODE_APPEND) {
                out.append(data + "\n");
            }else {
                out.write(data);
            }

            // Close stream writer.
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDirectoryExternalPrivate(String mainDir, String subFolder){
        return mContext.getExternalFilesDir(mainDir) + File.separator  + subFolder;
    }

    public String readFileExternalPrivate(String mainDir, String subFolder, String fileName) {

        try {

            String directory = getDirectoryExternalPrivate(mainDir, subFolder);
            File folder = new File(directory);

            File file = new File(folder, fileName);

            // If file doesn't exist.
            if (!file.exists()) {
                Log.e(TAG, "File doesn't exist.");
                return null;
            }

            FileInputStream fis = new FileInputStream(file);

            // Instantiate a buffer reader. (Buffer )
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(fis));

            String s;
            StringBuilder fileContentStrBuilder = new StringBuilder();

            // Read every lines in file.
            while ((s = bufferedReader.readLine()) != null) {
                fileContentStrBuilder.append(s);
            }

            // Close buffer reader.
            bufferedReader.close();

            return fileContentStrBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }

    public boolean deleteFileExternalPrivate(String mainDir, String subFolder, String fileName){

        String directory = getDirectoryExternalPrivate(mainDir, subFolder);
        File folder = new File(directory);

        File file = new File(folder, fileName);

        // If file doesn't exist.
        if (!file.exists()) {
            Log.e(TAG, "File doesn't exist.");
            return true;
        }
        return file.delete();
    }

    // Looking for File directory of all external cards (including onboard sd card).
    public ArrayList<File> getExternalSDCardDirectory(){

        // Retrieve the primary External Storage (usually onboard sd card, it's based on user setting).
        final File primaryExternalStorage = Environment.getExternalStorageDirectory();

        // Primary external storage (onboard sd card) usually has path: [storage]/emulated/0
        File externalStorageRoot = primaryExternalStorage.getParentFile().getParentFile();

        // Get list folders under externalStorageRoot (which includes primaryExternalStorage folder).
        File[] files = externalStorageRoot.listFiles();

        ArrayList<File> listStorage = new ArrayList<>();

        for (File file : files) {
            // it is a real directory (not a USB drive)...
            if ( file.isDirectory() && file.canRead() && (file.listFiles().length > 0) ) {
                listStorage.add(file);
            }
        }

        return listStorage;
    }


    /**********************************************************************************************/

    public File getExternalPublicDirectory(String... args){
        try {
            String path = buildPathPublicDirectory(args);
            if (path == null)
                return null;

            File dir = Environment.getExternalStoragePublicDirectory(path);

            if (!dir.exists())
                return dir.mkdirs() ? dir : null;

            return dir;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private String buildPathPublicDirectory(String... args){
        if (args != null && args.length > 0){
            StringBuilder sb = new StringBuilder();
            sb.append(args[0]);
            for (int i = 1; i < args.length; i++){
                sb.append(File.separator).append(args[i]);
            }
            return sb.toString();
        }
        return null;
    }

    public boolean writeFile(@NonNull File destDirectory, String fileName, String data){
        try{
            if(destDirectory.exists()) {
                File file = new File(destDirectory, fileName);

                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter out = new OutputStreamWriter(fos);

                out.write(data);
                out.close();
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public String readFile(File directory, String fileName){
        try {
            // Create file.
            File file = new File(directory, fileName);
            return readFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String readFile(File file) throws IOException{
        FileInputStream fis = new FileInputStream(file);

        // Instantiate a buffer reader. (Buffer )
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(fis));

        String s;
        StringBuilder fileContentStrBuilder = new StringBuilder();

        // Read every lines in file.
        while ((s = bufferedReader.readLine()) != null) {
            fileContentStrBuilder.append(s);
        }

        // Close buffer reader.
        bufferedReader.close();

        return fileContentStrBuilder.toString();
    }

    public boolean deleteFile(@NonNull File file){
        if(file.exists()) {
            return file.delete();
        }
        return false;
    }
}
