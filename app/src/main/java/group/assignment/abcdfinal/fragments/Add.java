package group.assignment.abcdfinal.fragments;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import group.assignment.abcdfinal.R;
import group.assignment.abcdfinal.adapter.GalleryAdapter;
import group.assignment.abcdfinal.model.GalleryImages;

public class Add extends Fragment {

    private EditText descET;
    private ImageView imageView;
    private RecyclerView recyclerView;
    private ImageButton backBtn, nextBtn;
    private List<GalleryImages> list;
    private GalleryAdapter adapter;
    private FirebaseUser user;

    Uri imageUri;

    public Add(){
        //Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        //Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){

        super.onViewCreated(view,savedInstanceState);

        init(view);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        recyclerView.setHasFixedSize(true);

        list = new ArrayList();
        adapter = new GalleryAdapter(list);

        recyclerView.setAdapter(adapter);

        clickListener();

    }

    private void clickListener(){
        adapter.SendImage(new GalleryAdapter.SendImage() {
            @Override
            public void onSend(Uri picUri) {
                imageUri = picUri;

                CropImage.activity(picUri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(4,3)
                        .start(getContext(), Add.this);
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FirebaseStorage storage = FirebaseStorage.getInstance();
                final StorageReference storageReference = storage.getReference().child("Post Images/"+System.currentTimeMillis());
                storageReference.putFile(imageUri)
                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                if (task.isSuccessful()){
                                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            String imageURL = uri.toString();

                                            uploadData(uri.toString());
                                        }
                                    });
                                }
                            }
                        });

            }
        });
    }

    private void uploadData(String imaURL){
        CollectionReference reference = FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUid()).collection("Post Images");

        String id = reference.document().getId();

        String description = descET.getText().toString();

        Map<String, Object> map = new HashMap<>();
        map.put("id",id);
        map.put("description",description);
        map.put("imageUrl", imaURL);
        map.put("timestamp", FieldValue.serverTimestamp());
        map.put("userName", user.getDisplayName());
        map.put("profileImage", String.valueOf(user.getPhotoUrl()));
        map.put("likeCount", 0);
        map.put("uid", user.getUid());

        reference.document(id).set(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                          if (task.isSuccessful()){
                              System.out.println();
                          }else
                              Toast.makeText(getContext(),"Error" + task.getException().getMessage(),
                                      Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void init(View view){

        descET = view.findViewById(R.id.descriptionET);
        imageView = view.findViewById(R.id.imageView);
        recyclerView = view.findViewById(R.id.recyclerView);
        backBtn = view.findViewById(R.id.backBtn);
        nextBtn = view.findViewById(R.id.nextBtn);

        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Dexter.withContext(getContext())
                        .withPermissions(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(new MultiplePermissionsListener(){
                            @Override
                            public void onPermissionsChecked(MultiplePermissionsReport report) {
                                    if (report.areAllPermissionsGranted()){
                                        File file = new File(Environment.getExternalStorageDirectory().toString()+"/Downloads");

                                        if (file.exists()){
                                            File[] files = file.listFiles();

                                                assert files != null;
                                                for (File file1 : files){
                                                    if (file1.getAbsolutePath().endsWith(". jpg") || file1.getAbsolutePath().endsWith("png")){
                                                        list.add(new GalleryImages(Uri.fromFile(file1)));
                                                        adapter.notifyDataSetChanged();
                                                    }
                                                }
                                        }
                                    }
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {

                            }
                        }).check();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            Uri image = data.getData();

            Glide.with(getContext())
                    .load(image)
                    .into(imageView);
            imageView.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.VISIBLE);
        }
    }
}