package com.example.filecompressor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_COMPRESS = 1;
    private static final int REQUEST_CODE_DECOMPRESS = 2;
    private static final int REQUEST_PERMISSION_READ_WRITE = 100;

    private TextView resultTextView;
    private HuffmanCompressor.HuffmanNode huffmanTree;
    private Map<Character, String> codebook;
    private String selectedFilePath;
    private int currentRequestCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);
        Button compressButton = findViewById(R.id.compressButton);
        Button decompressButton = findViewById(R.id.decompressButton);

        compressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRequestCode = REQUEST_CODE_COMPRESS;
                requestPermissions();
            }
        });

        decompressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentRequestCode = REQUEST_CODE_DECOMPRESS;
                requestPermissions();
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Build.VERSION.SDK_INT >= 34) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                    }, REQUEST_PERMISSION_READ_WRITE);
                } else {
                    selectFile(currentRequestCode);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    }, REQUEST_PERMISSION_READ_WRITE);
                } else {
                    selectFile(currentRequestCode);
                }
            }
        } else {
            selectFile(currentRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_READ_WRITE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile(currentRequestCode);
            } else {
                Toast.makeText(this, "Permissions are required to proceed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectFile(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                selectedFilePath = getFilePath(uri);
                if (selectedFilePath != null) {
                    if (requestCode == REQUEST_CODE_COMPRESS) {
                        compressFile();
                    } else if (requestCode == REQUEST_CODE_DECOMPRESS) {
                        decompressFile();
                    }
                } else {
                    Toast.makeText(this, "Unable to get the file path", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void compressFile() {
        try {
            byte[] fileBytes = HuffmanCompressor.readFile(selectedFilePath);
            Map<Character, Integer> frequencyTable = HuffmanCompressor.buildFrequencyTable(fileBytes);
            huffmanTree = HuffmanCompressor.buildHuffmanTree(frequencyTable);
            codebook = new HashMap<>();
            HuffmanCompressor.generateHuffmanCodes(huffmanTree, "", codebook);
            String encodedText = HuffmanCompressor.encodeFile(fileBytes, codebook);
            String compressedFilePath = selectedFilePath + ".huffman";
            HuffmanCompressor.saveEncodedFile(encodedText, compressedFilePath);
            resultTextView.setText("File compressed and saved to: " + compressedFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            resultTextView.setText("Compression failed: " + e.getMessage());
        }
    }

    private void decompressFile() {
        try {
            String encodedText = HuffmanCompressor.readEncodedFile(selectedFilePath);
            if (huffmanTree != null) {
                byte[] decodedBytes = HuffmanCompressor.decodeFile(encodedText, huffmanTree);
                String decompressedFilePath = selectedFilePath.replace(".huffman", "_decompressed");
                HuffmanCompressor.writeFile(decodedBytes, decompressedFilePath);
                resultTextView.setText("File decompressed and saved to: " + decompressedFilePath);
            } else {
                resultTextView.setText("Please compress a file first.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            resultTextView.setText("Decompression failed: " + e.getMessage());
        }
    }

    private String getFilePath(Uri uri) {
        String filePath = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(this, uri)) {
            String wholeID = DocumentsContract.getDocumentId(uri);
            String id = wholeID.split(":")[1];
            String[] column = {android.provider.MediaStore.Files.FileColumns.DATA};
            String sel = android.provider.MediaStore.Files.FileColumns._ID + "=?";
            android.database.Cursor cursor = getContentResolver().query(android.provider.MediaStore.Files.getContentUri("external"), column, sel, new String[]{id}, null);
            int columnIndex = cursor.getColumnIndex(column[0]);
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }
            cursor.close();
        } else {
            filePath = uri.getPath();
        }
        return filePath;
    }
}
