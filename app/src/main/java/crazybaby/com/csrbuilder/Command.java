package crazybaby.com.csrbuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.csr.gaia.android.library.Gaia;
import com.csr.gaia.android.library.GaiaCommand;
import com.csr.gaia.android.library.GaiaLink;
import com.csr.gaia.android.library.Gaia.EventId;
import com.csr.gaia.android.library.Gaia.Status;
import com.csr.gaia.android.library.GaiaLink.Transport;

public class Command {
	Handler handler;

	private Context context;;// 0=连接失败，1连接成功,3=出错/4=文件出错

	public static enum HANDLId {
		CONNECTED,
		DISCONNECTED,
		SELECTDFU,
		CRAFTREAD,
		TRANSPORTIONPROGRESS,
		STARTTRANSPORT,
		TRANSPORTFINISH,
		TRANSPORTFAIL,

		VERIFYING,
		VERIFYFAIL,
		UPGRADING, ;

		public static HANDLId valueOf(int id) {
			if (id < 0 || id >= HANDLId.values().length)
				return null;

			return HANDLId.values()[id];
		}
	}

	public Command(Handler handler, Context context) {
		// TODO Auto-generated constructor stub
		this.handler = handler;
		this.context = context;
	}

	private GaiaLink gaiaLink;

	public void init() {
		gaiaLink = new GaiaLink(GaiaLink.Transport.BT_GAIA);
		gaiaLink.setReceiveHandler(mHandler);
	}

	public void Connect(String addresss) {
		try {
			gaiaLink.connect(addresss);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void disconnect(){
		try {
		if (gaiaLink!=null){

				gaiaLink.disconnect();

		}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CharSequence[] getList() {

		return gaiaLink.getClientList();
	}

	public void start() {
		startDfu();
	}

	public void sendCsrCommand(int command) {
		try {
			gaiaLink.sendCommand(Gaia.VENDOR_CSR, command);
		} catch (IOException e) {
		}
	}

	Handler mHandler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			Message msgs = handler.obtainMessage();
			switch (GaiaLink.Message.valueOf(msg.what)) {
				case CONNECTED:

					msgs.what = HANDLId.CONNECTED.ordinal();
					handler.sendMessage(msgs);
					break;
				case DISCONNECTED:
					msgs.what = HANDLId.DISCONNECTED.ordinal();
					handler.sendMessage(msgs);
					break;
				case ERROR:
					// state=3;
					// msgs.what=state;
					// handler.sendMessage(msgs);
					break;
				// A packet of unframed data has been sent
				case STREAM:

					anotherChunkDone();
					break;

				case UNHANDLED:
					GaiaCommand command = (GaiaCommand) msg.obj;
					if (command.getCommandId() == Gaia.COMMAND_EVENT_NOTIFICATION) {
						EventId event_id = command.getEventId();
						switch (event_id) {
							case CHARGER_CONNECTION:
								Log.i("CHARGER_CONNECTION",
										"不处理" + command.getCommandId());
								break;

							case USER_ACTION:
								Log.i("USER_ACTION", "不处理" + command.getCommandId());
								// statusFragment.showUserEvent(uaName(user_action));
								break;

							case DFU_STATE:
								// dfuFragment.setDfuState(command.getByte(1));
								Log.i("DFU_STATE", "不处理" + command.getByte(1));
								int dfu_state = command.getByte(1);
								switch (dfu_state) {
									case 0:
										Log.i("DFU_STATE", "DFU download starting");

										hurl();
										break;

									case 1:
										msgs.what = HANDLId.TRANSPORTFAIL.ordinal();
										handler.sendMessage(msgs);
										Log.i("DFU_STATE", " DFU: download failure");
										break;

									case 2:
										msgs.what = HANDLId.VERIFYING.ordinal();
										handler.sendMessage(msgs);

										Log.i("DFU_STATE", " DFU: verifying image");
										// dfu_status.setText("DFU: verifying image");
										// dfu_progress.setIndeterminate(true);
										// dfu_writing = false;
										break;

									case 3:
										msgs.what = HANDLId.VERIFYFAIL.ordinal();
										handler.sendMessage(msgs);
										Log.i("DFU_STATE",
												" DFU: image verification failure");
										break;

									case 4:
										msgs.what = HANDLId.UPGRADING.ordinal();
										handler.sendMessage(msgs);

										Log.i("DFU_STATE", " DFU: upgrade");
										break;

									default:
										Log.i("DFU_STATE", "DFU state " + dfu_state);
										break;
								}

								break;

							default:
								Log.i("DFU Event", "不处理" + command.getByte(1));
								// toast("DFU Event " + event_id.toString());
								break;
						}

						sendAcknowledgement(command, Gaia.Status.SUCCESS,
								event_id.ordinal());
					} else if (command.getCommandId() == Gaia.COMMAND_DFU_REQUEST) {

						sendAcknowledgement(command, Gaia.Status.SUCCESS);
						msgs.what = HANDLId.CRAFTREAD.ordinal();
						handler.sendMessage(msgs);
						craftisok=true;

					}

					else {
						Log.i("收到命令", "不处理" + command.getCommandId());
						sendAcknowledgement(command, Gaia.Status.NOT_SUPPORTED);
					}

					break;
			}

		};

		;
	};


	private  boolean craftisok=false;
	// public void requesttoUdf(){
	// activityAccess.launchDfu(mDfuFile.getAbsolutePath(), mCrc);
	// }
	private void sendAcknowledgement(GaiaCommand command, Status status,
									 int... payload) {
		//

		try {
			gaiaLink.sendAcknowledgement(command, status, payload);

		}

		catch (IOException e) {
		}
	}

	public void abortDfu(String reason) {
		if (reason == null) {
			Toast.makeText(context.getApplicationContext(),
					"DFU: download failure====", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(context.getApplicationContext(),
					"DFU: download failure===" + reason, Toast.LENGTH_SHORT)
					.show();
		}

		dfu_writing = false;
	}
	public boolean setUdfIs(InputStream inputStream) throws IOException {
		try {
			mCrc = DfuCrc.fileCrc(inputStream);
		} catch (IOException e) {
			mCrc = 0;
		}
		if (mCrc == 0) {
			// state=3;
			// Message msgs=mHandler.obtainMessage();
			// msgs.what=state;
			// handler.sendMessage(msgs);
			// activityAccess.toast("Failed to calculate CRC on file.");
			return false;
		} else {
			// Swap words because that's how dfuBegin wants it.
			mCrc = ((mCrc & 0xFFFFL) << 16) | ((mCrc & 0xFFFF0000L) >> 16);
//			mDfuFile = file;
			dfu_stream=inputStream;
			return true;
		}
	}

	public boolean setUdfFile(File file) {
		try {
			mCrc = DfuCrc.fileCrc(file);
		} catch (IOException e) {
			mCrc = 0;
		}
		if (mCrc == 0) {
			// state=3;
			// Message msgs=mHandler.obtainMessage();
			// msgs.what=state;
			// handler.sendMessage(msgs);
			// activityAccess.toast("Failed to calculate CRC on file.");
			return false;
		} else {
			// Swap words because that's how dfuBegin wants it.
			mCrc = ((mCrc & 0xFFFFL) << 16) | ((mCrc & 0xFFFF0000L) >> 16);
			mDfuFile = file;
			return true;
		}
	}

	public boolean  startDfu() {

		if (!craftisok) {
			Log.i("startDfu", "craft is not ok");
			return false;
		}
		try {
			Log.i("向飞碟发指令=====", "sendAcknowledgement:" + "dfu 开始传输");
			Message msgs = handler.obtainMessage();
			msgs.what = HANDLId.STARTTRANSPORT.ordinal();
			handler.sendMessage(msgs);
			int size=0;
			// Toast.makeText(context.getApplicationContext(),"开始传输"
			// ,Toast.LENGTH_SHORT).show();
			if (mDfuFile!=null){
			 size = (int) mDfuFile.length();
			}else if (dfu_stream!=null){
			size=dfu_stream.available();
			}
			chunks_total = (size + CHUNK_SIZE - 1) / CHUNK_SIZE;
			chunks_done = 0;
			if (mDfuFile!=null) {
				dfu_stream = new FileInputStream(mDfuFile);
			}
			gaiaLink.dfuBegin(size, (int) mCrc);
			return true;

		}

		catch (IOException e) {
			Toast.makeText(context.getApplicationContext(),
					"DFU FAIL!\n" + e.toString(), Toast.LENGTH_LONG).show();
			e.printStackTrace();
			return false;
		}

	}

	private InputStream dfu_stream;
	private File mDfuFile = null;
	private long mCrc = 0;
	private int chunks_total = 0;
	private int chunks_done = 0;
	private boolean dfu_writing = true;// 是否正在写
	private static final int CHUNK_SIZE = 240;

	public void anotherChunkDone() {

		++chunks_done;

		if (dfu_writing) {
			hurl();
		}
	}

	protected void hurl() {
		byte[] chunk = new byte[CHUNK_SIZE];
		Message msgs = handler.obtainMessage();

		try {
			int length = dfu_stream.read(chunk);
			if (length > 0) {
				msgs.what = HANDLId.TRANSPORTIONPROGRESS.ordinal();

				msgs.arg1 = chunks_done;
				msgs.arg2 = chunks_total;
				handler.sendMessage(msgs);

				gaiaLink.sendRaw(chunk, length);
			} else {

				msgs.what = msgs.what = HANDLId.TRANSPORTFINISH.ordinal();

				handler.sendMessage(msgs);

				dfu_stream.close();
			}
		}

		catch (Exception e) {
			e.printStackTrace();
		}

	}

	public int getAllSize() {
		return chunks_total;
	}

	private String hexw(int i) {
		return String.format("%04x", i);
	}
}
