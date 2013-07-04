package com.quickblox.chat_v2.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.quickblox.chat_v2.R;
import com.quickblox.chat_v2.adapters.NewDialogAdapter;
import com.quickblox.chat_v2.core.ChatApplication;
import com.quickblox.chat_v2.interfaces.OnDialogCreateComplete;
import com.quickblox.chat_v2.interfaces.OnUserProfileDownloaded;
import com.quickblox.chat_v2.utils.ContextForDownloadUser;
import com.quickblox.chat_v2.utils.GlobalConsts;
import com.quickblox.chat_v2.widget.TopBar;
import com.quickblox.core.QBCallbackImpl;
import com.quickblox.core.result.Result;
import com.quickblox.module.users.QBUsers;
import com.quickblox.module.users.model.QBUser;
import com.quickblox.module.users.result.QBUserPagedResult;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA. User: Andrew Dmitrenko Date: 4/11/13 Time: 5:07
 * PM
 */
public class NewDialogActivity extends Activity implements AdapterView.OnItemClickListener, OnDialogCreateComplete, OnUserProfileDownloaded {

    private TopBar topBar;
    private ListView contactListView;
    private Button searchBtn;
    private TextView contactName;

    private NewDialogAdapter newDialogAdapter;

    private ProgressDialog progress;

    private ChatApplication app;
    private String createdDialogId;
    private int tUserId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ChatApplication.getInstance();
        app.getMsgManager().setDialogCreateListener(this);
        setContentView(R.layout.new_dialog_layout);
        initViews();
    }

    private void initViews() {
        topBar = (TopBar) findViewById(R.id.top_bar);
        topBar.setFragmentParams(TopBar.NEW_DIALOG_ACTIVITY, View.INVISIBLE, false);
        contactListView = (ListView) findViewById(R.id.contacts_listView);
        contactListView.setOnItemClickListener(this);
        searchBtn = (Button) findViewById(R.id.search_button);
        searchBtn.setOnClickListener(searchBtnClickListener);
        contactName = (EditText) findViewById(R.id.contact_name);
    }

    View.OnClickListener searchBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!TextUtils.isEmpty(contactName.getText().toString())) {
                searchUserOnServer();
                switchProgressDialog(true, NewDialogActivity.this.getResources().getString(R.string.new_dialog_activity_search_user));
            }
        }
    };

    private void searchUserOnServer() {
        QBUsers.getUsersByFullName(contactName.getText().toString(), new QBCallbackImpl() {
            @Override
            public void onComplete(Result result) {
                if (result.isSuccess()) {
                    refreshContactList(((QBUserPagedResult) result).getUsers());
                } else {
                    refreshContactList(new ArrayList<QBUser>());
                    Toast.makeText(NewDialogActivity.this, getResources().getString(R.string.dialog_activity_reject), Toast.LENGTH_LONG).show();
                }
                NewDialogActivity.this.switchProgressDialog(false, new String());
            }
        });
    }

    private void refreshContactList(ArrayList<QBUser> qbUsers) {
        if (qbUsers != null) {
            newDialogAdapter = new NewDialogAdapter(this, qbUsers);
            contactListView.setAdapter(newDialogAdapter);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        QBUser user = (QBUser) adapterView.getItemAtPosition(i);
        app.getMsgManager().setDialogCreateListener(this);
        app.getMsgManager().createDialog(user, true);
        switchProgressDialog(true, NewDialogActivity.this.getResources().getString(R.string.new_dialog_activity_create_dialog));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.getMsgManager().setDialogCreateListener(null);
        app.getQbm().removeUserProfileListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }

    @Override
    public void dialogCreate(int userId, String customObjectUid) {
        createdDialogId = customObjectUid;
        tUserId = userId;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                app.getQbm().addUserProfileListener(NewDialogActivity.this);
                app.getQbm().getSingleUserInfo(tUserId, ContextForDownloadUser.DOWNLOAD_FOR_DIALOG);
            }
        });

    }

    @Override
    public void downloadComlete(QBUser friend, ContextForDownloadUser pContextForDownloadUser) {
        if (pContextForDownloadUser == ContextForDownloadUser.DOWNLOAD_FOR_DIALOG) {
            app.getDialogsUsersMap().put(String.valueOf(friend.getId()), friend);
            finishActivityReceivedResult(friend.getId(), createdDialogId);
        }
        app.getQbm().removeUserProfileListener(this);
    }

    public void switchProgressDialog(boolean enable, String progressText) {

        if (enable) {
            progress = ProgressDialog.show(NewDialogActivity.this, getString(R.string.app_name), progressText, true);
        } else {
            if (progress.isShowing() && progress != null) {
                progress.dismiss();
            }
        }
    }

    private void finishActivityReceivedResult(int userId, String dialogId) {
        switchProgressDialog(false, new String());
        Intent intent = new Intent();
        intent.putExtra(GlobalConsts.USER_ID, String.valueOf(userId));
        intent.putExtra(GlobalConsts.DIALOG_ID, dialogId);
        intent.putExtra(GlobalConsts.PREVIOUS_ACTIVITY, GlobalConsts.DIALOG_ACTIVITY);
        setResult(RESULT_OK, intent);
        finish();

    }
}
