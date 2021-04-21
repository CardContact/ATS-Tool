/**
 * Tool to show ATS and other ISO 14443 communication parameter with Identive uTrust Reader
 *
 * Copyright (c) 2021, CardContact Systems GmbH, Minden, Germany
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of CardContact Systems GmbH nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CardContact Systems GmbH BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * BSD-3 License
 */

package de.cardcontact.atstool;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

public class ATSTool {

	private static final byte FEATURE_ESCAPE = 0x13;
	private static final String HEXCHARS = "0123456789ABCDEF";
	private final static String[] bps = new String[] { "106Kbps", "212Kbps", "424Kbps", "848Kbps" };
	private String readerName = null;
	private boolean listReaders = false;
	private TerminalFactory factory;
	private CardTerminal ct;
	private Card card;
	int controlCode;


	public ATSTool() {
		factory = TerminalFactory.getDefault();
	}



	private String hexify(byte[] buf) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buf.length; i++) {
			sb.append(HEXCHARS.charAt((buf[i] & 0xF0) >> 4));
			sb.append(HEXCHARS.charAt(buf[i] & 0x0F));
		}
		return sb.toString();
	}



	/**
	 * Decode command line arguments
	 *
	 * @param args Arguments passed on the command line
	 * @return true if arguments valid
	 */
	private boolean decodeArgs(String[] args) {
		int i = 0;

		while (i < args.length) {
			if (args[i].equals("-r")) {
				readerName = args[++i];
			} else if (args[i].equals("-l")) {
				listReaders = true;
			}
			i++;
		}

		return true;
	}



	private void listReaders() throws CardException {
		List<CardTerminal> list = factory.terminals().list();

		System.out.println("Available card terminals:");
		for (CardTerminal ct : list) {
			System.out.println(" " + ct.getName());
		}
	}



	/**
	 * Determine the IOCTL for the requested feature
	 * @param feature One of PCSCIOControl.FEATURE_*
	 * @return The feature control code or -1 if feature was not found
	 */
	private int getFeatureControlCode(byte feature) {
		byte[] features;

		int ioctl = (0x31 << 16 | (3400) << 2);

		byte[] empty = {};

		try {
			features = card.transmitControlCommand(ioctl, empty);
		}
		catch(CardException e) {
			return -1;
		}

		int i = 0;

		while(i < features.length) {
			if (features[i] == feature) {
				int c = 0;
				i += 2;
				for (int l = 0; (i < features.length) && (l < 4); i++, l++) {
					c <<= 8;
					c |= features[i] & 0xFF;
				}

				return c;

			} else {
				i += 6; // skip six bytes
			}
		}

		return -1;
	}



	private void cardInfo() throws CardException {
		byte[] rsp = card.transmitControlCommand(controlCode, new byte[] { (byte)0x11 });
//		System.out.println("RAW: " + hexify(rsp));

		String rate = "106";
		if ((rsp[1] & 0x01) == 0x01) {
			rate += ",212";
		}
		if ((rsp[1] & 0x02) == 0x02) {
			rate += ",424";
		}
		if ((rsp[1] & 0x04) == 0x04) {
			rate += ",848";
		}
		System.out.println("Reader to card rates " + rate);

		rate = "106";
		if ((rsp[1] & 0x10) == 0x10) {
			rate += ",212";
		}
		if ((rsp[1] & 0x20) == 0x20) {
			rate += ",424";
		}
		if ((rsp[1] & 0x40) == 0x40) {
			rate += ",848";
		}
		System.out.println("Card to reader rates " + rate);

		if ((rsp[1] & 0x80) == 0x80) {
			System.out.println("Same rate in both direction");
		} else {
			System.out.println("Different rates in both direction");
		}

		String abtype = (rsp[2] & 0x0F) == 0 ? "Type A" : "Type B";
		switch(rsp[2] & 0xF0) {
		case 0x00:
			System.out.println("Memory card (" + abtype + ")");
			break;
		case 0x10:
			System.out.println("T=CL ISO 14443-4 card (" + abtype + ")");
			break;
		case 0x20:
			System.out.println("Dual mode card (" + abtype + ")");
			break;
		default:
			System.out.println("Unknown card type (" + abtype + ")");
		}

	}



	private void cardATS() throws CardException {
		byte[] rsp = card.transmitControlCommand(controlCode, new byte[] { (byte)0x93 });
		System.out.println("ATS: " + hexify(rsp));
	}



	private void cardBPS() throws CardException {
		byte[] rsp = card.transmitControlCommand(controlCode, new byte[] { (byte)0x9E });
//		System.out.println("RAW: " + hexify(rsp));
		System.out.println("Rates reader:card " + bps[(rsp[0] & 0xF0) >> 4] + ":" + bps[(rsp[0] & 0x0F)]);
	}



	private void run(String[] args) {
		decodeArgs(args);

		try {
			if (listReaders) {
				listReaders();
				return;
			}

			CardTerminal ct;
			if (readerName != null) {
				ct = factory.terminals().getTerminal(readerName);
			} else {
				ct = factory.terminals().list().get(0);
			}

			System.out.println("Using reader \"" + ct.getName() + "\"");
			card = ct.connect("*");

			controlCode = getFeatureControlCode(FEATURE_ESCAPE);
			if (controlCode == -1) {
				System.out.println("Reader does not support Escape commands or Escape commands have been disabled in the OS.");
				System.out.println("On Linux edit /etc/libccid_Info.plist and change ifdDriverOptions to 0x0001.");
				System.out.println("On Windows see http://msdn.microsoft.com/en-us/windows/hardware/gg487509.aspx.");
				System.exit(1);
			}
			cardInfo();
			cardATS();
			cardBPS();
		} catch (CardException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	public static void main(String[] args) {
		ATSTool t = new ATSTool();
		t.run(args);
	}
}
