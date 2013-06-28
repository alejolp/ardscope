package tr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JPanel;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

public class PanelDibujo extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int SAMPLE_COUNT = 512;
	
	private int[] valores = null;
	private int valoresUltPos = 0;
	private FloatFFT_1D fft = null;
	private float[] fftData = null;
	private int fftDataSpikeIdx = 0;
	private int prevValor = 0;
	private int inicioFFT = 0;
	
	public int getFftDataSpikeIdx() {
		return fftDataSpikeIdx;
	}
	
	public int getFFTSize() {
		return SAMPLE_COUNT;
	}
	
	public void setInicioFFT(int x) {
		inicioFFT = x;
	}

	public void actualizarTamanioFFT(int tamanio) {
		SAMPLE_COUNT = tamanio;
		valores = new int[tamanio];
		fftData = new float[tamanio];
		fft = new FloatFFT_1D(tamanio);
		valoresUltPos = 0;
		fftDataSpikeIdx = 0;
	}
	
	@Override
	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Rectangle vr = getVisibleRect();
		int inicio = (valoresUltPos - vr.width + SAMPLE_COUNT) % SAMPLE_COUNT;
		float phaseScale = 20.0f;
		float magScale = 6.0f;
		float spikePhase;
		
		g2d.setColor(Color.BLACK);
		g2d.fill(getVisibleRect());

		g2d.setColor(Color.BLUE);
		g2d.fillRect(0, vr.height / 2, vr.width, 1);

		g2d.setColor(Color.BLUE);
		g2d.fillRect(0, (int) (vr.height - 1 - phaseScale * Math.PI), vr.width, 1);

		g2d.setColor(Color.GRAY);
		g2d.fillRect(fftDataSpikeIdx / 2- inicioFFT, 0, 1, vr.height - 1);
		
		g2d.setColor(Color.YELLOW);
		int x, x0;
		int xant = 0, yant = vr.height - 1;
		
		for (x = (inicio + SAMPLE_COUNT) % SAMPLE_COUNT, x0 = 0; x != valoresUltPos; x = (x + 1) % SAMPLE_COUNT, x0++) {
			int y = vr.height / 2 - valores[x];
			// g2d.fillRect(x0, y, 1, 1);
			g2d.drawLine(xant, yant, x0, y);
			xant = x0;
			yant = y;
		}
		
		g2d.setColor(Color.GREEN);
		
		int limite = (vr.width);
		int inicioFFTLocal = inicioFFT;
		
		spikePhase = (float) Math.atan2(fftData[fftDataSpikeIdx], fftData[fftDataSpikeIdx + 1]);
		xant = 0;
		yant = vr.height - 1;
		
		for (x = inicioFFTLocal * 2, x0 = 0; x < SAMPLE_COUNT && x0 < limite; x += 2, x0++) {
			float phase = (float) Math.atan2(fftData[x], fftData[x+1]);
			
			phase -= spikePhase;
			if (phase > Math.PI) 
				phase -= Math.PI;
			else if (phase < (-1 * Math.PI))
				phase += Math.PI;
			
			int y = (int) (vr.height - 1 - phaseScale * phase - phaseScale * Math.PI);
			if (y < 0) y = 0;
			g2d.drawLine(xant, yant, x0, y);
			//g2d.fillRect(x, y, 1, 1);
			xant = x0;
			yant = y;
		}

		g2d.setColor(Color.RED);
		
		xant = 0;
		yant = vr.height - 1;
		
		for (x = inicioFFTLocal * 2, x0 = 0; x < SAMPLE_COUNT && x0 < limite; x += 2, x0++) {
			float mag = (float) Math.hypot(fftData[x], fftData[x+1]);
			mag = (float) (magScale * 10.0f * Math.log10(mag));
			int y = (int) (vr.height - 1 - mag);
			if (y < 0) y = 0;
			g2d.drawLine(xant, yant, x0, y);
			xant = x0;
			yant = y;
		}
	}

	public int[] getValores() {
		return valores;
	}

	public void setValores(int[] valores) {
		this.valores = valores;
	}

	public int getValoresUltPos() {
		return valoresUltPos;
	}

	public void setValoresUltPos(int valoresUltPos) {
		this.valoresUltPos = valoresUltPos;
	}
	
	public void nuevoValor(int v) {
		valores[valoresUltPos] = (v + prevValor) / 2;
		prevValor = v;
		valoresUltPos = (valoresUltPos + 1) % valores.length;
	}
	
	public void actualizarFFT() {
		/*
		 * Update FFT data.
		 */
		
		int x, x0;
		
		for (x0 = 0, x = valoresUltPos + 1; x0 < SAMPLE_COUNT; ++x0, ++x) {
			fftData[x0] = valores[x % SAMPLE_COUNT];
		}
		
		fft.realForward(fftData);
		
		// Find the fundamental spike ignoring DC.
		
		int maxpos = 0;
		float maxval = 0;
		for (x=10; x<SAMPLE_COUNT; x += 2) {
			float mag = (float) Math.hypot(fftData[x], fftData[x+1]);
			if (mag > maxval) {
				maxval = mag;
				maxpos = x;
			}
		}
		
		fftDataSpikeIdx = maxpos;
	}
}
