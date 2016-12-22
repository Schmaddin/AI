import java.awt.AWTException;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.awt.event.*;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.SwingDispatchService;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;

public class Listener extends JFrame implements NativeKeyListener,
		NativeMouseListener, WindowListener {

	static Listener listener;
	static int lastClickX = 0;
	static int lastClickY = 0;

	static long lastActivationTime = 0;

	static long lastRightClick = 0;

	public Listener() {
		// Set the event dispatcher to a swing safe executor service.
		GlobalScreen.setEventDispatcher(new SwingDispatchService());

		setTitle("Child: ");
		setSize(250, 400);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		addWindowListener(this);
		setVisible(true);

		listener = this;
		setResizable(false);
		
		GridLayout experimentLayout = new GridLayout(0, 2);
		setLayout(experimentLayout);

		JButton title = createButton("Random Title.: ", "Take the chance");
		add(title);

		JButton shortD = createButton("Short Description",
				"Rshort short short");
		add(shortD);

		JButton middleD = createButton("Middle Description",
				"Rshort short short");
		add(middleD);
		
		JButton longD = createButton("Long Description",
				"Rshort short short");
		add(longD);
		
		JButton largeD = createButton("largeD Description",
				"Rshort short short");
		add(largeD);
		
		JButton keys = createButton("Keys",
				"key key key key");
		add(keys);
		
		JButton street = createButton("Street.: ", "Rossendorf 35");
		add(street);
		
		JButton plz = createButton("PLZ.: ", "90556");
		add(plz);
		
		JButton company = createButton("Company.: ", "Martin Würflein Freelancing");
		add(company);

		JButton buttonAdd = createButton("Adress",
				"Rossendorf 35, 90556 Cadolzburg");
		add(buttonAdd);
		

		
		JButton city = createButton("City.: ", "Cadolzburg");
		add(city);
		
		JButton mail = createButton("Mail.:", "martin.wuerflein@gmx.de");
		add(mail);
		
		JButton mail2 = createButton("Mail2.:", "martin.wuerflein@gmx.de");
		add(mail2);

		JButton bTel = createButton("Tel.:", "0173-1903127");
		add(bTel);

		JButton bFax = createButton("Fax.: ", "");
		add(bFax);
		

		JButton buttonFullName = createButton("Name", "Martin Würflein");
		add(buttonFullName);

	}

	private JButton createButton(String buttonName, String content) {
		JButton button = new JButton(buttonName);

		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				copyToClipoardAndAction(content);

			}

			private void copyToClipoardAndAction(String toClipboard) {
				copyToClipboard(toClipboard);

				listener.setVisible(false);

				Robot bot;
				try {
					bot = new Robot();
					int mask = InputEvent.BUTTON1_DOWN_MASK;

					bot.mouseMove(lastClickX, lastClickY);
					bot.mousePress(mask);
					bot.mouseRelease(mask);
					bot.keyPress(KeyEvent.VK_META);
					bot.keyPress(KeyEvent.VK_A);

					bot.keyRelease(KeyEvent.VK_META);
					bot.keyRelease(KeyEvent.VK_A);

					lastActivationTime = System.currentTimeMillis();

				} catch (AWTException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		return button;
	}

	public void windowOpened(WindowEvent e) {
		// Initialze native hook.
		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException ex) {
			System.err
					.println("There was a problem registering the native hook.");
			System.err.println(ex.getMessage());
			ex.printStackTrace();

			System.exit(1);
		}

		GlobalScreen.addNativeKeyListener(this);
		GlobalScreen.addNativeMouseListener(this);
	}

	public void windowClosed(WindowEvent e) {
		// Clean up the native hook.
		try {
			GlobalScreen.unregisterNativeHook();
		} catch (NativeHookException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.runFinalization();
		System.exit(0);
	}

	public void windowClosing(WindowEvent e) { /* Unimplemented */
	}

	public void windowIconified(WindowEvent e) { /* Unimplemented */
	}

	public void windowDeiconified(WindowEvent e) { /* Unimplemented */
	}

	public void windowActivated(WindowEvent e) { /* Unimplemented */
	}

	public void windowDeactivated(WindowEvent e) { /* Unimplemented */
	}

	public void nativeKeyPressed(NativeKeyEvent e) {
		System.out.println("Key Pressed: "
				+ NativeKeyEvent.getKeyText(e.getKeyCode()));

		if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
			try {
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// == <<
		if (e.getKeyCode() == 41) {

			if (lastActivationTime + 500 < System.currentTimeMillis()) {
				bringAppToFront();
			} else {

			}

			lastActivationTime = System.currentTimeMillis();

		}
	}

	private void bringAppToFront() {
		BringSelfToFocus();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listener.setVisible(true);
				// listener.toFront();
				System.out.println("unser listener: ");
				Point p = MouseInfo.getPointerInfo().getLocation();
				listener.setLocation(p.x - 30, p.y - 30);

				lastClickX = p.x;
				lastClickY = p.y;
			}
		});
	}

	public void nativeKeyReleased(NativeKeyEvent e) {
		System.out.println("Key Released: "
				+ NativeKeyEvent.getKeyText(e.getKeyCode()));

	}

	public void nativeKeyTyped(NativeKeyEvent e) {
		System.out.println("Key Typed: " + e.getKeyText(e.getKeyCode()));
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Listener();
			}
		});
	}

	private static void BringSelfToFocus() {
		AppleScript("tell me to activate");
	}

	private static String AppleScript(String script) {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("AppleScript");

		try {
			return (String) engine.eval(script);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void copyToClipboard(String text) {
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
	}

	@Override
	public void nativeMouseClicked(NativeMouseEvent event) {
		if (event.getButton() == 2) {
			if (lastRightClick + 400 > System.currentTimeMillis())
				bringAppToFront();

			lastRightClick = System.currentTimeMillis();
		}

	}

	@Override
	public void nativeMousePressed(NativeMouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nativeMouseReleased(NativeMouseEvent arg0) {
		// TODO Auto-generated method stub

	}

}