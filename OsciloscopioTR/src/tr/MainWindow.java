package tr;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;

public class MainWindow extends JFrame implements SerialPortEventListener, Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1L;

	private PanelDibujo panelPpal = new PanelDibujo();
	private JComboBox opcionPuertoSerie = null;
	private JComboBox opcionTamanioFFT = null;
	private JScrollBar opcionInicioFFT = null;
	
	private Object mutexSerialPort = new Object();
	private CommPortIdentifier portId = null;
	private SerialPort serialPort = null;
	private InputStream serialInputStream = null;
	
	private int sampleRate = 10000;
	
	private Thread thread;
	
	public MainWindow() {
		super("Osciloscopio");
				
		opcionPuertoSerie = new JComboBox(buscarListaDePuertosSerie());
		opcionTamanioFFT = new JComboBox(new Object[]{"512", "1024", "2048", "4096", "8192", "16384", "32768", "65536"});
		opcionInicioFFT = new JScrollBar(JScrollBar.HORIZONTAL);
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(320, 240));
		setLayout(new BorderLayout());
		
		add(panelPpal, BorderLayout.CENTER);
		
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				int tamanioFFT = Integer.parseInt((String)opcionTamanioFFT.getSelectedItem());
				opcionInicioFFT.setMaximum(Math.max(tamanioFFT / 2 - getContentPane().getWidth(), 0));
			}
		});
		
		panelPpal.addMouseListener(new MouseInputAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				actualizarTitulo();
			}
		});
		
		JPanel panelDerecho = new JPanel();
		panelDerecho.setLayout(new BoxLayout(panelDerecho, BoxLayout.Y_AXIS));
		
		opcionPuertoSerie.setMaximumSize(opcionPuertoSerie.getPreferredSize());
		opcionPuertoSerie.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				actualizarPuerto();
			}
		});
		
		opcionTamanioFFT.setMaximumSize(opcionTamanioFFT.getPreferredSize());
		opcionTamanioFFT.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				actualizarTamanioFFT();
			}
		});
		
		opcionInicioFFT.addAdjustmentListener(new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(AdjustmentEvent arg0) {
				synchronized (mutexSerialPort) {
					panelPpal.setInicioFFT(opcionInicioFFT.getValue());
				}
			}
		});
		
		panelDerecho.add(opcionPuertoSerie);
		panelDerecho.add(opcionTamanioFFT);
		panelDerecho.add(Box.createVerticalGlue());
		
		add(panelDerecho, BorderLayout.LINE_END);
		add(opcionInicioFFT, BorderLayout.PAGE_END);
		
		pack();
		setSize(new Dimension(640, 480));

		actualizarTamanioFFT();
		actualizarPuerto();
		
		thread = new Thread(this);
		thread.start();
	}
	
	public void actualizarTitulo() {
		double n = (panelPpal.getFftDataSpikeIdx() / 2) * (sampleRate / 2) / (double)panelPpal.getFFTSize();
		setTitle("Pos: " + String.format("%.2f", n) + " Hz");
	}
	
	private String[] buscarListaDePuertosSerie() {
		@SuppressWarnings("rawtypes")
		Enumeration e = CommPortIdentifier.getPortIdentifiers();
		ArrayList<String> lista = new ArrayList<String>();

		while (e.hasMoreElements()) {
			CommPortIdentifier cid = (CommPortIdentifier)e.nextElement();
			lista.add(cid.getName());
		}
		
		 //new String[]{"/dev/ttyUSB0", "/dev/ttyUSB1", "/dev/ttyUSB2", });
		return lista.toArray(new String[]{});
	}
	
	public void actualizarPuerto() {
		String valor = (String) opcionPuertoSerie.getItemAt(opcionPuertoSerie.getSelectedIndex());
		System.out.println("Abriendo " + valor);
		
		synchronized(mutexSerialPort) {
			if (serialPort != null) {
				try {
					serialInputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				serialPort.close();
				serialPort = null;
				serialInputStream = null;
			}
			
			try {
				portId = CommPortIdentifier.getPortIdentifier(valor);
				serialPort = (SerialPort) portId.open("Osciloscopio", 5000);
				serialPort.setSerialPortParams(115200,
		                SerialPort.DATABITS_8,
		                SerialPort.STOPBITS_1,
		                SerialPort.PARITY_NONE);
	
				serialPort.addEventListener(this);
				serialInputStream = serialPort.getInputStream();
				serialPort.notifyOnDataAvailable(true);
				
			} catch (NoSuchPortException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PortInUseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TooManyListenersException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedCommOperationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void actualizarTamanioFFT() {
		synchronized(mutexSerialPort) {
			int tamanioFFT = Integer.parseInt((String)opcionTamanioFFT.getSelectedItem());
			opcionInicioFFT.setMinimum(0);
			opcionInicioFFT.setMaximum(Math.max(tamanioFFT / 2 - getContentPane().getWidth(), 0));
			opcionInicioFFT.setValue(0);
			panelPpal.actualizarTamanioFFT(tamanioFFT);
			panelPpal.setInicioFFT(0);
		}
	}
	
	
	@Override
	public void serialEvent(SerialPortEvent arg0) {
		/*
		 * CUIDADO: esto se ejecuta en un thread propio de RXTX.
		 */
		try {
			synchronized(mutexSerialPort) {
		
				switch (arg0.getEventType()) {
				case SerialPortEvent.BI:
				case SerialPortEvent.OE:
				case SerialPortEvent.FE:
				case SerialPortEvent.PE:
				case SerialPortEvent.CD:
				case SerialPortEvent.CTS:
				case SerialPortEvent.DSR:
				case SerialPortEvent.RI:
				case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
					break;
				case SerialPortEvent.DATA_AVAILABLE:
					byte[] readBuffer = new byte[128];
		
					try {
						while (serialInputStream.available() > 0) {
							int numBytes = serialInputStream.read(readBuffer);
							for (int i = 0; i < numBytes; ++i) {
								// Complemento-a-2 para valores negativos, son todos positivos ;)
								panelPpal.nuevoValor((readBuffer[i] < 0) ? (~readBuffer[i] + 1) : readBuffer[i]);
							}
						}
					} catch (IOException e) {
						serialPort.notifyOnDataAvailable(false);
						serialPort.removeEventListener();
						
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								actualizarPuerto();
							}
						});
					}
					break;
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				synchronized(mutexSerialPort) {
					panelPpal.actualizarFFT();
				}
				
				SwingUtilities.invokeAndWait(new Runnable() {

					@Override
					public void run() {
						actualizarTitulo();
						panelPpal.repaint();
					}
				});
				
				Thread.sleep(1000 / 40);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MainWindow w = new MainWindow();
		w.setVisible(true);
	}


}
