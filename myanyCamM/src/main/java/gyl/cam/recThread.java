package gyl.cam;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.myanycam.bean.VideoData;
import com.myanycamm.cam.AppServer;
import com.myanycamm.cam.CallAcceptActivity;
import com.myanycamm.ui.CloudLivingView;
import com.myanycamm.ui.LivingView;
import com.myanycamm.utils.ELog;
import com.myanycamm.utils.FileUtils;
import com.myanycamm.utils.Utils;

public class recThread extends Thread {
	private static String TAG = "recThread";
	String ip;//
	int port;
	// CloudLivingView root;
	Socket socket = null;
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	DataOutputStream outbuf;
	Boolean _quit;
	int total;//
	int ptr;
	byte[] rec;//
	int recType = -1;
	Bitmap bitmap;//
	private Handler mHandler;

	int noDataCnt;//

	boolean _isFir = true;
	int width;
	int height;
	int type;
	int parseTime;// 接一帧视频的时间
	int netRecevieTime = 0;// 网络发一帧视频的时间
	int sleepTime;// 需要延时的时间
	int apapterTime = 5;// 调整时间,默认5毫秒

	int[] rgba = new int[720 * 480 + 1];

	public native void init(int type, int width, int height);

	public native int DecodeFrame(Bitmap bitmap, byte[] in, int insize);

	public native void testFrame();

	static {
		System.loadLibrary("ffmpegutils");
	}

	public recThread(Handler mHandler) {
		// root = parent;
		this.mHandler = mHandler;
	}

	public void init() {
		// bitmap = Bitmap.createBitmap(352, 288, Bitmap.Config.ARGB_8888);//
		ELog.i("gyl", "解析初始化");
		_isFir = true;
		total = 0;
		ptr = 0;
		_quit = false;
		// ip = root.ip;
		// port = root.port;
		noDataCnt = 0;
		// conToSer(ip, port);

	}

	@Override
	public void run() {

		int i = 0;
		AppServer.isDisplayVideo = true;
		while (AppServer.isDisplayVideo) {
			if (null == VideoData.Videolist || VideoData.Videolist.isEmpty()) {
				//
				if (VideoData.audioArraryList.isEmpty()) {
					//
					// try {
					// recThread.sleep(1000);
					// i++;
					// if(i==10){
					// root.isDisplayVideo = false;
					// root.mHandler.sendEmptyMessage(VideoPlayActivity.NO_VIDEO);
					// }
					// } catch (InterruptedException e) {
					//
					// e.printStackTrace();
					// }
				}

			} else {
				try {
					parseByteData(VideoData.Videolist.get(0).getVideoData());
					VideoData.Videolist.remove(0);
					if (VideoData.Videolist.size() > 100) {
						VideoData.Videolist.clear();
					}

				} catch (NullPointerException e) {
					ELog.i(TAG, "解码时空指针异常");
					VideoData.Videolist.remove(0);
					continue;
				} catch (IndexOutOfBoundsException e) {
					//
				}

			}

		}
	}

	void close() {
		while (total != 0) {
			try {
				sleep(100);
				Log.e("gyl", "wait the thread to close");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (socket.isConnected()) {
			try {
				socket.shutdownInput();
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (socket.isClosed()) {
				Log.e("gyl", "socket closed");
			}
		}
	}

	void parse() {
		// Log.e("gyl", "parse");

		//

		if (rec[6] != recType) {
			recType = rec[6];
			ELog.i("gyl", "_isFir" + _isFir);
			_isFir = false;
			if (rec[1] == 0x00) {
				Log.e("gyl", "mpeg code");
				type = 0;
			}
			if (rec[1] == 0x01) {
				Log.e("gyl", "h264 code");
				type = 1;
			}
			if (rec[6] == 0x00) {
				bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888);//
				width = 160;
				height = 120;
			}
			if (rec[6] == 0x01) {
				bitmap = Bitmap.createBitmap(176, 144, Bitmap.Config.ARGB_8888);//
				width = 176;
				height = 144;
			}
			if (rec[6] == 0x02) {
				bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888);//
				width = 320;
				height = 240;
			}
			if (rec[6] == 0x03) {
				bitmap = Bitmap.createBitmap(352, 288, Bitmap.Config.ARGB_8888);//
				width = 352;
				height = 288;
			}
			if (rec[6] == 0x04) {
				bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);//
				width = 640;
				height = 480;
			}
			if (rec[6] == 0x05) {
				bitmap = Bitmap.createBitmap(720, 480, Bitmap.Config.ARGB_8888);//
				width = 720;
				height = 480;
			}
			if (rec[6] == 0x06) {
				bitmap = Bitmap
						.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);//
				width = 1280;
				height = 720;
			}
			if (rec[6] == 0x07) {
				bitmap = Bitmap
						.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);//
				width = 1920;
				height = 1080;
			}
			if (rec[6] == 0x08) {
				bitmap = Bitmap
						.createBitmap(640, 360, Bitmap.Config.ARGB_8888);//
				width = 640;
				height = 360;
			}
			if (rec[6] == 0x09) {
				bitmap = Bitmap
						.createBitmap(320,180, Bitmap.Config.ARGB_8888);//
				width = 320;
				height = 180;
			}
			ELog.i(TAG, "width:" + width + "height:" + height);
			init(type, width, height);
		}

		byte[] bmp = new byte[total - 7];
		for (int i = 0; i < total - 7; i++) {
			bmp[i] = rec[i + 7];
		}
		long parseTimeBefore = System.currentTimeMillis();

		int n = DecodeFrame(bitmap, bmp, total - 7);
		//
		// FileUtils.saveByteToFile(yuv,
		// Environment.getExternalStorageDirectory().getPath()+"/my.yuv");
		// VideoData.yuvArrayList.add(yuv);
		long parseTimeAfter = System.currentTimeMillis();
		parseTime = (int) (parseTimeAfter - parseTimeBefore);
		//
		ELog.i(TAG, "解码n:" + n);
		if (n > 0) {
			// bitmap.setPixels(rgba, 0, width , 0, 0,
			// width, height);
			mHandler.sendMessage(Message.obtain(mHandler, 21, bitmap));
			// mHandler.sendEmptyMessage(21);

		} else {
			mHandler.sendEmptyMessage(30);
		}

	}

	void conToSer(String ip, int port) {
		Log.e(ip, "" + port);
		try {
			Log.e("gyl", "11111111111111");
			socket = new Socket();
			Log.e("gyl", "22222222222222");
			InetSocketAddress remoteAddr = new InetSocketAddress(ip, port);
			Log.e("gyl", "33333333333333");
			socket.connect(remoteAddr, 10000);
			if (socket.isConnected()) {
				Log.e("gyl", "socket is connected");
				req();
			}
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
		} catch (IOException e) {
			// TODO Auto-generated catch block
		}
	}

	int getSize() {
		int availbytes = 0;

		try {
			availbytes = socket.getInputStream().available();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return availbytes;
	}

	void req() throws IOException {
		try {
			outbuf = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// header
		byte[] req = new byte[8];

		req[0] = 0x00;
		req[1] = 0x00;
		req[2] = 0x00;
		req[3] = 0x00;
		req[4] = 0x00;
		req[5] = 0x00;
		req[6] = 0x00;
		req[7] = 0x00;
		outbuf.write(req);
	}

	public void parseByteData(byte[] videoData) {
		//
		if (videoData == null) {
			return;
		}
		this.total = videoData.length;
		this.rec = new byte[total];
		this.rec = videoData;
		parse();
	}

	public byte[] int2byte(int res) {
		byte[] targets = new byte[4];
		targets[3] = (byte) (res & 0xff);// 最低位
		targets[2] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[1] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[0] = (byte) (res >>> 24);// 最高位,无符号右移。
		return targets;
	}

	public int Byte2Int(byte[] b) {
		int bb;
		bb = (b[0] & 0x000000FF) << 24 | (b[1] & 0x000000FF) << 16
				| (b[2] & 0x000000FF) << 8 | (b[3] & 0x000000FF);
		return bb;
	}

}
