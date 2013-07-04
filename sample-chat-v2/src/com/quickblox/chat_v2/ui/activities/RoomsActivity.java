package com.quickblox.chat_v2.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.quickblox.chat_v2.R;
import com.quickblox.chat_v2.adapters.RoomListAdapter;
import com.quickblox.chat_v2.core.ChatApplication;
import com.quickblox.chat_v2.interfaces.OnRoomListDownloaded;
import com.quickblox.chat_v2.utils.GlobalConsts;
import com.quickblox.module.chat.QBChat;
import com.quickblox.module.chat.listeners.RoomListener;
import com.quickblox.module.chat.model.QBChatRoom;
import com.quickblox.module.custom.model.QBCustomObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA. User: Andrew Dmitrenko Date: 11.04.13 Time: 9:58
 */
public class RoomsActivity extends Activity implements RoomListener, OnRoomListDownloaded {

    private ListView roomListLv;
    private Button newRoomBtn;
    private RoomListAdapter roomListAdapter;
    private RoomListAdapter.RoomViewHolder roomHolder;
    private TextView mEmptyRooms;

    private QBChatRoom chatRoom;
    private String roomName;

    private ChatApplication app;
    private static final int REQUEST_NEW_ROOM = 0;

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);
        setContentView(R.layout.rooms_layout);
        app = ChatApplication.getInstance();

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        app.getMsgManager().setRoomListDownloadListener(this);
        app.getMsgManager().downloadPersistentRoom();
    }

    private void initViews() {

        roomListLv = (ListView) findViewById(R.id.room_list_lv);
        roomListLv.setOnItemClickListener(itemClick);
        newRoomBtn = (Button) findViewById(R.id.new_room_btn);
        newRoomBtn.setOnClickListener(newRoomBtnClickListener);
        mEmptyRooms = (TextView) findViewById(R.id.empty_rooms_label);
        applyRoomList(app.getUserPresentRoomList());
    }

    private View.OnClickListener newRoomBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (!app.getContactsMap().isEmpty()) {
                Intent intent = new Intent(RoomsActivity.this, NewRoomActivity.class);
                startActivityForResult(intent, REQUEST_NEW_ROOM);
            } else {
                Toast.makeText(RoomsActivity.this, getString(R.string.new_room_activity_block_create_room), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_NEW_ROOM:
                if (resultCode != Activity.RESULT_CANCELED) {
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtras(data.getExtras());
                    startActivity(intent);
                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void applyRoomList(final List<QBCustomObject> roomList) {

        roomListAdapter = new RoomListAdapter(this, (ArrayList) roomList);
        roomListLv.setAdapter(roomListAdapter);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                roomListAdapter.notifyDataSetChanged();
                if (((ArrayList) roomList).size() > 0) {
                    roomListLv.setVisibility(View.VISIBLE);
                    mEmptyRooms.setVisibility(View.INVISIBLE);
                } else {
                    roomListLv.setVisibility(View.INVISIBLE);
                    mEmptyRooms.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private OnItemClickListener itemClick = new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

            QBCustomObject co = (QBCustomObject) parent.getItemAtPosition(position);
            roomName = co.getFields().get(GlobalConsts.ROOM_NAME).toString();
            QBChat.getInstance().joinRoom(roomName, app.getQbUser(), RoomsActivity.this);

            roomHolder = ((RoomListAdapter.RoomViewHolder) v.getTag());
            roomHolder.connectinRoomProgress.setVisibility(View.VISIBLE);

        }


    };

    private void saveRoomAndStartActivity() {
        Intent i = new Intent(RoomsActivity.this, ChatActivity.class);
        i.putExtra(GlobalConsts.PREVIOUS_ACTIVITY, GlobalConsts.ROOM_ACTIVITY);
        i.putExtra(GlobalConsts.ROOM_NAME, roomName);
        RoomsActivity.this.startActivity(i);
    }

    @Override
    public void roomListDownloaded() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                applyRoomList(app.getUserPresentRoomList());
            }
        });

    }

    @Override
    public void onCreatedRoom(QBChatRoom pRoom) {

    }

    @Override
    public void onJoinedRoom(QBChatRoom pRoom) {
        chatRoom = pRoom;
        chatRoom.addMessageListener(app.getMsgManager());
        app.setJoinedRoom(chatRoom);
        saveRoomAndStartActivity();
        roomHolder.connectinRoomProgress.setVisibility(View.INVISIBLE);

    }
}
