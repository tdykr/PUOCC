package adrean.thesis.puocc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReceiptConfirmationPayActivity extends AppCompatActivity {

    int totalPrice = 0;
    List<String> listCartId = new ArrayList<>();
    Toolbar toolbar;

    UserPreference mUserPreferences;
    UserModel userModel;
    ListAdapter adapter;
    String id,date;
    private DecimalFormat df = new DecimalFormat("#,###.##");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_confirmation_pay);

        String pattern = "dd-MM-yyyy";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        date = simpleDateFormat.format(new Date());

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Order Confirmation");

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView totPrice= (TextView) findViewById(R.id.totalPrice);
        TextView custName = (TextView) findViewById(R.id.custName);
        TextView address = (TextView) findViewById(R.id.address);
        ListView listItem = (ListView) findViewById(R.id.list);
        Button confirmBtn = (Button) findViewById(R.id.btnConfirm);

        Intent in = getIntent();
        List<Map<String,String>> data = (List<Map<String, String>>) in.getSerializableExtra("data");

        mUserPreferences = new UserPreference(this);
        userModel = mUserPreferences.getUser();

        String totalMedicine = "";

        for(Map<String, String> mapData : data){
            String price = mapData.get("PRICE");
            String med = mapData.get("MED_NAME") ;
            String cartId = mapData.get("CART_ID");
            String quantity = mapData.get("QUANTITY");

            listCartId.add(cartId);
            totalPrice += Integer.parseInt(price)*Integer.parseInt(quantity);
            totalMedicine += med;
        }

        ListAdapter adapter = new SimpleAdapter(
                this, data, R.layout.list_medicine,
                new String[]{"CATEGORY","MED_NAME","FORMATTED_MED_PRICE","QUANTITY","MEDICINE_PICT"},
                new int[]{R.id.medCategory, R.id.medName, R.id.medPrice, R.id.medQuantity, R.id.img});

        listItem.setAdapter(adapter);

        Log.d("totalPrice", String.valueOf(totalPrice));
        String formattedTotalPrice=getString(R.string.rupiah,df.format(totalPrice));

        totPrice.setText(formattedTotalPrice);
        custName.setText(userModel.getUserName());
        address.setText(userModel.getUserAddress());

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addOrder(listCartId);
            }
        });
    }

    public void addOrder(final List<String> cartID){

        class addOrder extends AsyncTask<Void,Void,String> {

            ProgressDialog loading;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                loading = ProgressDialog.show(ReceiptConfirmationPayActivity.this,"Uploading Data...","Please Wait...",false,false);
            }

            @Override
            protected void onPostExecute(final String s) {
                super.onPostExecute(s);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loading.dismiss();
                        Toast.makeText(ReceiptConfirmationPayActivity.this,s, Toast.LENGTH_LONG).show();
                    }
                }, 2000);
                final Intent intent = new Intent(ReceiptConfirmationPayActivity.this, DetailTransactionActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("TRANS_ID",id);
                intent.putExtra("DATE",date);
                intent.putExtra("STATUS","PENDING");
                intent.putExtra("TYPE","ONLINE");
                intent.putExtra("TOTAL_PRICE",Integer.toString(totalPrice));
                startActivity(intent);

            }

            @Override
            protected String doInBackground(Void... v) {

                HashMap<String,String> params = new HashMap<>();
                String res = "";
                RequestHandler rh = new RequestHandler();
                String totPrice = Integer.toString(totalPrice);
                UUID uuid = UUID.randomUUID();
                id = uuid.toString().replace("-","").toUpperCase();

                HashMap<String,String> param = new HashMap<>();
                param.put("UUID",id);
                param.put("USER",userModel.getUserName());
                param.put("TOTAL_PRICE",totPrice);
                param.put("TYPE","ONLINE");
                param.put("STATUS","PENDING");
                res = rh.sendPostRequest(phpConf.URL_ADD_TRANSACTION, param);

                for(String cartData : cartID){
                    params.put("ID",cartData);
                    params.put("USER",userModel.getUserName());
                    params.put("UUID",id);

                    res = rh.sendPostRequest(phpConf.URL_UPDATE_CART_ORDER, params);
                }
                return res;
            }
        }

        addOrder add = new addOrder();
        add.execute();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
