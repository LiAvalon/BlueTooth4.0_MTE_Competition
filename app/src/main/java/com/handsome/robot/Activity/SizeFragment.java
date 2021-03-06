package com.handsome.robot.Activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.handsome.robot.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.content.ContentValues.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link SizeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SizeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String IMAGE_UNSPECIFIED = "image/*";

    private final int IMAGE_CODE = 1; // 图库
    private final int REQUEST_CODE = 2; // 这里的IMAGE_CODE是自己任意定义的
    public final int TAKE_PHOTO_CODE = 3;//相机RequestCode

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    private static final int PERMISSION_REQUEST_CAMERA = 1;

    public final int TYPE_TAKE_PHOTO = 1;//Uri获取类型判断


    private Button btnSelectImage;
    private TextView tvL;
    private TextView tvC;
    private TextView tvR;
    private ImageView imagePhoto;

    private BleUtils sBleUtils;
    private NavigationActivity sNavActivity;
    private DrawerActivity sDrawerActivity;

    //拍照图片文件
    private File mediaFile;
    //图片文件与相关信息
    private TextView tvImageDistance;
    private TextView tvImageName;
    private TextView tvImageTime;
    private TextView tvImageLocation;
    private String imageLeftDistance;
    private String imageCenterDistance;
    private String imageRightDistance;
    //位置
    private Location location;
    private String latitude;
    private String longitude;

    //姿态角度
    private String directionAngle; //方向角，指向东南西北，绕Z轴角度
    private String slantAngle; //倾斜角，手机头部和尾部抬起的角度，绕X轴角度
    private String rotationAngle; //旋转角,手机左右侧抬起的角度，绕Y轴角度
    DecimalFormat decimalFormat=new DecimalFormat(".0");
    private TextView tvImagePosX;
    private TextView tvImagePosY;
    private TextView tvImagePosZ;

    //声明一个AlertDialog构造器
    private AlertDialog.Builder builder;


    // TODO: Rename and change types of parameters
    private String mParam1;

    /*private OnFragmentInteractionListener mListener;*/

    public SizeFragment() {
        // Required empty public constructor

    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment SizeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SizeFragment newInstance(String param1) {
        SizeFragment fragment = new SizeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_size, container, false);
        Bundle bundle = getArguments();
        //拍照按钮
        btnSelectImage = (Button) view.findViewById(R.id.btn_select_image);
        //照片显示
        imagePhoto = (ImageView) view.findViewById(R.id.image_photo);
        //实时显示左中右距离
        tvL = (TextView) view.findViewById(R.id.tv_left_distance);
        tvC = (TextView) view.findViewById(R.id.tv_center_distance);
        tvR = (TextView) view.findViewById(R.id.tv_right_distance);
        //照片的信息
        tvImageDistance = (TextView) view.findViewById(R.id.tv_image_distance);
        tvImageName = (TextView) view.findViewById(R.id.tv_image_name);
        tvImageTime = (TextView) view.findViewById(R.id.tv_image_time);
        tvImageLocation = (TextView) view.findViewById(R.id.tv_image_location);

        tvImagePosX = (TextView)view.findViewById(R.id.tv_image_posture_x);
        tvImagePosY = (TextView)view.findViewById(R.id.tv_image_posture_y);
        tvImagePosZ = (TextView)view.findViewById(R.id.tv_image_posture_z);

        btnSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //获取当前的经度纬度
                location = sNavActivity.getLocation();
                latitude = Double.toString(location.getLatitude());
                longitude = Double.toString(location.getLongitude());
                //判断有无读写SD卡权限
                takePhotos();

            }
        });

        imagePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               showSimpleDialog(view);
            }
        });

        return view;
    }

    //显示基本Dialog
    private void showSimpleDialog(View view) {
        builder=new AlertDialog.Builder(getContext());
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setTitle("Measurement");
        builder.setMessage("Are you sure to open the Measurement-Mode？");

        //监听下方button点击事件
        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getContext(),"Open",Toast.LENGTH_SHORT).show();
                imagePhoto.setDrawingCacheEnabled(true);//获取bm前执行，否则无法获取
                Bitmap bitmap = setimage(imagePhoto); //调用setimage方法，得到返回值bitmap
                byte[] bytes = Bitmap2Bytes(bitmap);
                Intent intent = new Intent(getActivity(),MeasureActivity.class);
                intent.putExtra("image",bytes);
                startActivity(intent);
                imagePhoto.setDrawingCacheEnabled(false);//获取bm后执行，以清空画图缓冲区，否则下一次从ImageView对象中获取的图像，
                //还是原来的图像。
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getContext(),"Cancel",Toast.LENGTH_SHORT).show();
            }
        });

        //设置对话框是可取消的
        builder.setCancelable(true);
        AlertDialog dialog=builder.create();
        dialog.show();
    }

    private void takePhotos() {
        /**1.在AndroidManifest文件中添加需要的权限。
         *
         * 2.检查权限
         *这里涉及到一个API，ContextCompat.checkSelfPermission，
         * 主要用于检测某个权限是否已经被授予，方法返回值为PackageManager.PERMISSION_DENIED
         * 或者PackageManager.PERMISSION_GRANTED。当返回DENIED就需要进行申请授权了。
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);

            }
            /*if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {*/
            if (getActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //权限没有被授予
                /**3.申请授权
                 * @param
                 *  @param activity The target activity.（Activity|Fragment、）
                 * @param permissions The requested permissions.（权限字符串数组）
                 * @param requestCode Application specific request code to match with a result（int型申请码）
                 *    reported to {@link OnRequestPermissionsResultCallback#onRequestPermissionsResult(
                 *int, String[], int[])}.
                 * */

                //requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {//权限被授予
                //TODO:进行图库的图片选择--这里我们需要改成拍照然后获取该图片
               /* Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_UNSPECIFIED);
                startActivityForResult(intent, IMAGE_CODE);*/
                //拍照
                Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri photoUri = getMediaFileUri(TYPE_TAKE_PHOTO);
                takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takeIntent, TAKE_PHOTO_CODE);

            }
        }
    }

    //把图片转换成bitmap形式传递通过intent形式传递过去
    private Bitmap setimage(ImageView view1){
        Bitmap image = ((BitmapDrawable)view1.getDrawable()).getBitmap();
        Bitmap bitmap1 = Bitmap.createBitmap(image);
        return bitmap1;
    }
    //bitmap转换成byte形式
    private byte[] Bitmap2Bytes(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    //获取图片保存的文件地址和名称
    public Uri getMediaFileUri(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "梅特勒");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        //创建Media File
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HH:mm:ss").format(new Date());
        File mediaFile;
        if (type == TYPE_TAKE_PHOTO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + longitude +"_" + latitude + "_" + timeStamp + ".jpg");
            setMediaFile(mediaFile);
        } else {
            return null;
        }
        return Uri.fromFile(mediaFile);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        //针对SD写访问申请的回调
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                /*//权限被授予
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, IMAGE_UNSPECIFIED);
                startActivityForResult(intent, IMAGE_CODE);*/

            } else {
                // Permission Denied
                Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        //对于系统相机申请的回调
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被授予
                Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                Uri photoUri = getMediaFileUri(TYPE_TAKE_PHOTO);
                takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takeIntent, TAKE_PHOTO_CODE);
            }
        } else {
            // Permission Denied
            Toast.makeText(getActivity(), "Permission Denied", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
       /* if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }*/
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        /*sDrawerActivity = (DrawerActivity) activity;*/
       /* sBleUtils = sNavActivity.getBleUtils();*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Bitmap bm = null;

        // 外界的程序访问ContentProvider所提供数据 可以通过ContentResolver接口

        ContentResolver resolver = getActivity().getContentResolver();
        //对于图库访问的回调
        if (requestCode == IMAGE_CODE) {
            try {
              /*  bm = MediaStore.Images.Media.getBitmap(resolver, originalUri);

                imagePhoto.setImageBitmap(ThumbnailUtils.extractThumbnail(bm, 300, 300));  //使用系统的一个工具类，
                // 参数列表为 Bitmap Width,Height  这里使用压缩后显示，否则在华为手机上ImageView 没有显示
                // 显得到bitmap图片
                // imageView.setImageBitmap(bm);*/
                //似乎这三句就够了。下面的cursor总是返回null
                Uri originalUri = data.getData(); // 获得图片的uri
                Bitmap bit = BitmapFactory.decodeStream(getActivity().getContentResolver().openInputStream(originalUri));
                //图片进行缩略显示-防止图片缓存溢出
                imagePhoto.setImageBitmap(ThumbnailUtils.extractThumbnail(bit, 300, 300));

                //下面的似乎不行-指针总是返回空
                /*String[] proj = {MediaStore.Images.Media.DATA};

                // 好像是android多媒体数据库的封装接口，具体的看Android文档
                Cursor cursor = resolver.query(originalUri, proj, null, null, null);

                // 按我个人理解 这个是获得用户选择的图片的索引值
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                // 将光标移至开头 ，这个很重要，不小心很容易引起越界
                cursor.moveToFirst();
                // 最后根据索引值获取图片路径
                String path = cursor.getString(column_index);

                imagePhoto.setImageBitmap(BitmapFactory.decodeFile(path));*/
            } catch (Exception e) {
                Log.e("TAG-->Error", e.toString());
            }
        }
        //对于拍照行为的回调
        if (requestCode == TAKE_PHOTO_CODE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    if (data.hasExtra("data")) {
                        Log.i("URI", "data is not null");
                        Bitmap bitmap = data.getParcelableExtra("data");
                        imagePhoto.setImageBitmap(bitmap);//imageView即为当前页面需要展示照片的控件，可替换
                    }
                } else {
                    Log.i("URI", "Data is null");
                    //获得拍照时候的左中右距离，文件名，拍照时间与经纬度坐标
                    File photoFile = getMediaFile();
                    String photoName = photoFile.getName();
                    String photoTime = photoName.split("_")[3].split("\\.")[0];
                    String photoLongitude = photoName.split("_")[1];
                    String photoLatitude = photoName.split("_")[2];
                    photoName = photoName.substring(0,3) +"_"+ photoTime.substring(9,17)+ ".jpg";
                    /*姿态处理*/
                   /* float[] angleValue = sNavActivity.getValues();
*/
                  /*  //方向角-绕Z轴角度，南方为0/360，顺时针增加度数。
                    directionAngle = decimalFormat.format(angleValue[0]+(float)180.00);
                    //倾斜角-绕X轴角度，水平面朝上为180°，面朝下360/0°，手机顶部往上翘起减少，对着你往后转增加度数
                    slantAngle = decimalFormat.format(angleValue[1]+(float)180.00);
                    //旋转角-绕Y轴角度，水平面朝上为180°，面朝下360/0°，手机左侧抬起旋转增加，顺时针增加度数
                    rotationAngle = decimalFormat.format(angleValue[2]+(float)180.00);*/

                   /* //方向角-绕Z轴角度，南方为0/360，顺时针增加度数。
                    directionAngle = decimalFormat.format(angleValue[0]+(float)180.00);
                    //倾斜角-绕X轴角度，水平面朝上为0°，面朝下+-180°，手机顶部往上翘起减少，对着你往后转增加度数
                    slantAngle = decimalFormat.format(angleValue[1]);
                    //旋转角-绕Y轴角度，水平面朝上为0°，面朝下+-180°，手机左侧抬起旋转增加，顺时针增加度数
                    rotationAngle = decimalFormat.format(angleValue[2]);*/



                    //更新UI-LCR Distance
                    imageLeftDistance = tvL.getText().toString();
                    imageCenterDistance = tvC.getText().toString();
                    imageRightDistance = tvR.getText().toString();
                    tvImageDistance.setText(imageLeftDistance + "-" + imageCenterDistance + "-" + imageRightDistance);
                    tvImageName.setText(photoName);
                    tvImageTime.setText(photoTime);



                    //将照片显示
                    Bitmap bitmap = BitmapFactory.decodeFile(getMediaFile().getPath());
                    imagePhoto.setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, 250, 250));//imageView即为当前页面需要展示照片的控件，可替换

                    //更新UI-经纬度和姿态
                    tvImageLocation.setText(photoLongitude + "-" + photoLatitude);
                    tvImagePosX.setText("X:" + slantAngle + "°" + " ");
                    tvImagePosY.setText("Y:" + rotationAngle + "°" + " ");
                    tvImagePosZ.setText("Z:" + directionAngle + " °" + " ");
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);


    }

    public TextView getTvL() {
        return tvL;
    }

    public TextView getTvC() {
        return tvC;
    }

    public TextView getTvR() {
        return tvR;
    }

    public File getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(File mediaFile) {
        this.mediaFile = mediaFile;
    }

    public void setLocation(Location location) {
        this.location = location;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
   /* public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }*/
}
