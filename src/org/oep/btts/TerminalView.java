package org.oep.btts;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.view.View;

public class TerminalView extends View {
	
	protected byte mBytes[] = new byte[16];
	
	public TerminalView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setBytes(List<Byte> bytes) {
		mBytes = new byte[bytes.size()];
		for(int i = 0; i < bytes.size(); i++) {
			mBytes[i] = bytes.get(i);
		}
		
		postInvalidate();
	}
	
	public void onDraw(Canvas canvas) {
		Paint paint = new Paint();
		paint.setColor(DEFAULT_COLOR);
		paint.setAntiAlias(true);
		
		float fontHeight = paint.getTextSize();
		
		int spacing = (getWidth() - 2 * HORIZONTAL_MARGINS) / 8;
		
		for(int i = 0; i*8 < mBytes.length && i * fontHeight < getHeight(); i++) {
			for(int j = 0; i*8 + j < mBytes.length && j < 8; j++) {
				int k = i*8 + j;
				byte b = mBytes[k];
				String hex = getHex(b);
				
				float y = VERTICAL_MARGINS + (i+1) * fontHeight;
				float x = HORIZONTAL_MARGINS + j * spacing;
				canvas.drawText(hex, x, y, paint);
			}
		}
	}
	
	protected String getHex(byte b) {
		String s = Integer.toHexString(0xFF & b);
		if(s.length() < 2) s = "0" + s;
		return s.toUpperCase();
	}
	
	protected boolean isPrintable(char c) {
		return c >= 32 && c < 127;
	}
	
	public static final int
		DEFAULT_COLOR = Color.WHITE,
		VERTICAL_MARGINS = 10,
		HORIZONTAL_MARGINS = 5;
}
