package com.hawkins.dmanager.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.hawkins.dmanager.network.http.WebProxy;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AutoProxyHandler {
	

	private ScriptEngine engine;
	private boolean init;
	private String pacUrl;
	private boolean pacLoaded;
	private StringBuffer autoProxyScript;

	private static AutoProxyHandler _this;

	private AutoProxyHandler() throws IOException, ScriptException {
	}

	public void setPacUrl(String pacUrl) {
		this.pacUrl = pacUrl;
		this.pacLoaded = false;
	}

	public static AutoProxyHandler getInstance() throws Exception {
		if (_this == null) {
			_this = new AutoProxyHandler();
		}
		return _this;
	}

	public WebProxy getProxyForUrl(String url) {
		log.info("Calling getProxyForUrl('" + url + "')");
		try {
			URL u = new URL(url);
			ProxyInfo info = findProxyForUrl(url, u.getHost());
			if (info == null || info.isDirect()) {
				return null;
			} else {
				log.info("Proxy (" + info.getProxy() + ":" + info.getPort() + ") for url: " + url);
				return new WebProxy(info.getProxy(), info.getPort());
			}
		} catch (Exception e) {
			log.info(e.getMessage());
		}
		return null;
	}

	public String getPacUrl() {
		return pacUrl;
	}

	private ProxyInfo findProxyForUrl(String url, String host)
			throws NoSuchMethodException, ScriptException, IOException {
		if (!pacLoaded) {
			loadPac();
		}
		Object localObject = ((Invocable) engine).invokeFunction("FindProxyForURL", new Object[] { url, host });
		System.out.println(localObject);
		ProxyInfo[] arr = extractAutoProxySetting((String) localObject);
		if (arr == null || arr.length < 1)
			return null;
		return arr[0];
	}

	private final ProxyInfo[] extractAutoProxySetting(String paramString) {
		if (paramString != null) {
			StringTokenizer localStringTokenizer = new StringTokenizer(paramString, ";", false);
			ProxyInfo[] arrayOfProxyInfo = new ProxyInfo[localStringTokenizer.countTokens()];
			int i = 0;
			while (localStringTokenizer.hasMoreTokens()) {
				String str = localStringTokenizer.nextToken();
				int j = str.indexOf("PROXY");
				if (j != -1) {
					arrayOfProxyInfo[(i++)] = new ProxyInfo(str.substring(j + 6));
				} else {
					j = str.indexOf("SOCKS");
					if (j != -1) {
						arrayOfProxyInfo[(i++)] = new ProxyInfo(null, str.substring(j + 6));
					} else {
						arrayOfProxyInfo[(i++)] = new ProxyInfo(null, -1);
					}
				}
			}
			return arrayOfProxyInfo;
		}
		return new ProxyInfo[] { new ProxyInfo(null) };
	}

	private String loadPacScript() throws IOException {
		log.info("Loading PAC script");
		InputStream pacStram = new URL(pacUrl).openStream();
		StringBuilder sb = new StringBuilder();
		byte[] buf = new byte[512];
		while (true) {
			int x = pacStram.read(buf);
			if (x == -1) {
				break;
			}
			sb.append(new String(buf, 0, x));
		}
		log.info("Done loading PAC script");
		return sb.toString();
	}

	private void loadPac() throws IOException, ScriptException {
		if (!init) {
			init();
			init = true;
		}
		String pacScript = loadPacScript();
		engine.eval(pacScript);
		pacLoaded = true;
	}

	private void init() throws ScriptException, IOException {
		log.info("Initializing PAC Handler");
		ScriptEngineManager mgr = new ScriptEngineManager(null);
		engine = mgr.getEngineByName("js");
		engine.put("obj", this);

		this.autoProxyScript = new StringBuffer();
		this.autoProxyScript.append(
				"var _mon = new Array('JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC');\nvar _day = new Array('SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT');\nfunction _isGmt(i) {\n return typeof i == 'string' && i == 'GMT'; }");
		this.autoProxyScript.append(
				"function dnsDomainIs(host, domain) {\nif (domain != null && domain.charAt(0) != '.')\nreturn shExpMatch(host, domain);\nreturn shExpMatch(host, '*' + domain); }");
		this.autoProxyScript.append("function isPlainHostName(host){\nreturn (dnsDomainLevels(host) == 0); }");
		this.autoProxyScript.append(
				"function convert_addr(ipchars) {\n    var bytes = ipchars.split('.');\n    var result = ((bytes[0] & 0xff) << 24) |\n                 ((bytes[1] & 0xff) << 16) |\n                 ((bytes[2] & 0xff) <<  8) |\n                  (bytes[3] & 0xff);\n    return result;\n}\n");
		this.autoProxyScript.append(
				"function isInNet(ipaddr, pattern, maskstr) {\n    var ipPattern = /^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$/;\n    var test = ipaddr.match(ipPattern);\n    if (test == null) {\n        ipaddr = dnsResolve(ipaddr);\n        if (ipaddr == null)\n            return false;\n    } else if ((test[1] > 255) || (test[2] > 255) || \n               (test[3] > 255) || (test[4] > 255) ) {\n        return false;\n    }\n    var host = convert_addr(ipaddr);\n    var pat  = convert_addr(pattern);\n    var mask = convert_addr(maskstr);\n    return ((host & mask) == (pat & mask));\n    \n}\n");
		this.autoProxyScript.append("function dnsResolve(host){\n return String(obj.dnsResolve(host));\n }");
		this.autoProxyScript.append("function isResolvable(host){\nreturn (dnsResolve(host) != ''); }");
		this.autoProxyScript
				.append("function localHostOrDomainIs(host, hostdom){\nreturn shExpMatch(hostdom, host + '*'); }");
		this.autoProxyScript.append(
				"function dnsDomainLevels(host){\nvar s = host + '';\nfor (var i=0, j=0; i < s.length; i++)\nif (s.charAt(i) == '.')\nj++;\nreturn j; }");
		this.autoProxyScript.append("function myIpAddress(){\nreturn '");
		try {
			InetAddress localInetAddress = InetAddress.getLocalHost();
			this.autoProxyScript.append(localInetAddress.getHostAddress());
		} catch (Throwable localThrowable) {
			localThrowable.printStackTrace();
			this.autoProxyScript.append("127.0.0.1");
		}
		this.autoProxyScript.append("'; }");
		this.autoProxyScript.append(
				"function shExpMatch(str, shexp){ \n if (typeof str != 'string' || typeof shexp != 'string') return false; \n if (shexp == '*') return true; \n if (str == '' && shexp == '') return true; \n str = str.toLowerCase();\n shexp = shexp.toLowerCase();\n var index = shexp.indexOf('*');\n if (index == -1) { return (str == shexp); } \n else if (index == 0) { \n for (var i=0; i <= str.length; i++) { \n if (shExpMatch(str.substring(i), shexp.substring(1))) return true; \n } return false; } \nelse { \nvar sub = null, sub2 = null; \nsub = shexp.substring(0, index);\nif (index <= str.length) sub2 = str.substring(0, index); \nif (sub != '' && sub2 != '' && sub == sub2) { \nreturn shExpMatch(str.substring(index), shexp.substring(index)); }\nelse { return false; }\n} }");
		this.autoProxyScript.append(
				"function _dateRange(day1, month1, year1, day2, month2, year2, gmt){\nif (typeof day1 != 'number' || day1 <= 0 || typeof month1 != 'string' || typeof year1 != 'number' || year1 <= 0\n || typeof day2 != 'number' || day2 <= 0 || typeof month2 != 'string' || typeof year2 != 'number' || year2 <= 0\n || typeof gmt != 'boolean') return false; \nvar m1 = -1, m2 = -1;\nfor (var i=0; i < _mon.length; i++){\nif (_mon[i] == month1)\nm1 = i;\nif (_mon[i] == month2)\nm2 = i;\n}\nvar cur = new Date();\nvar d1 = new Date(year1, m1, day1, 0, 0, 0);\nvar d2 = new Date(year2, m2, day2, 23, 59, 59);\nif (gmt == true)\ncur = new Date(cur.getTime() - cur.getTimezoneOffset() * 60 * 1000);\nreturn ((d1.getTime() <= cur.getTime()) && (cur.getTime() <= d2.getTime()));\n}\nfunction dateRange(p1, p2, p3, p4, p5, p6, p7){\nvar cur = new Date();\nif (typeof p1 == 'undefined')\nreturn false;\nelse if (typeof p2 == 'undefined' || _isGmt(p2))\n{\nif ((typeof p1) == 'string')\nreturn _dateRange(1, p1, cur.getFullYear(), 31, p1, cur.getFullYear(), _isGmt(p2));\nelse if (typeof p1 == 'number' && p1 > 31)\nreturn _dateRange(1, 'JAN', p1, 31, 'DEC', p1, _isGmt(p2));\nelse {\nfor (var i=0; i < _mon.length; i++)\nif (_dateRange(p1, _mon[i], cur.getFullYear(), p1, _mon[i], cur.getFullYear(), _isGmt(p2)))\n return true;\nreturn false;\n}\n}\nelse if (typeof p3 == 'undefined' || _isGmt(p3))\n{\nif ((typeof p1) == 'string')\nreturn _dateRange(1, p1, cur.getFullYear(), 31, p2, cur.getFullYear(), _isGmt(p3));\nelse if (typeof p1 == 'number' && typeof p2 == 'number' && (p1 > 31 || p2 > 31))\nreturn _dateRange(1, 'JAN', p1, 31, 'DEC', p2, _isGmt(p3));\nelse \n{\nif ((typeof p2) == 'string')\n{\nreturn _dateRange(p1, p2, cur.getFullYear(), p1, p2, cur.getFullYear(), _isGmt(p3));\n}\nelse \n{\nfor (var i=0; i < _mon.length; i++)\nif (_dateRange(p1, _mon[i], cur.getFullYear(), p2, _mon[i], cur.getFullYear(), _isGmt(p3)))\nreturn true;\nreturn false;\n}\n}\n}\nelse if (typeof p4 == 'undefined' || _isGmt(p4))\nreturn _dateRange(p1, p2, p3, p1, p2, p3, _isGmt(p4));\nelse if (typeof p5 == 'undefined' || _isGmt(p5))\n{\nif (typeof p2 == 'number')\nreturn _dateRange(1, p1, p2, 31, p3, p4, _isGmt(p5));\nelse \nreturn _dateRange(p1, p2, cur.getFullYear(), p3, p4, cur.getFullYear(), _isGmt(p5))\n}\nelse if (typeof p6 == 'undefined')\nreturn false;\nelse \nreturn _dateRange(p1, p2, p3, p4, p5, p6, _isGmt(p7));\n}");
		this.autoProxyScript.append(
				"function timeRange(p1, p2, p3, p4, p5, p6, p7) {\nif (typeof p1 == 'undefined')\nreturn false;\nelse if (typeof p2 == 'undefined' || _isGmt(p2))\nreturn _timeRange(p1, 0, 0, p1, 59, 59, _isGmt(p2));\nelse if (typeof p3 == 'undefined' || _isGmt(p3))\nreturn _timeRange(p1, 0, 0, p2, 0, 0, _isGmt(p3));\nelse if (typeof p4 == 'undefined')\nreturn false;\nelse if (typeof p5 == 'undefined' || _isGmt(p5))\nreturn _timeRange(p1, p2, 0, p3, p4, 0, _isGmt(p5));\nelse if (typeof p6 == 'undefined')\nreturn false;\nelse \nreturn _timeRange(p1, p2, p3, p4, p5, p6, _isGmt(p7));\n}\nfunction _timeRange(hour1, min1, sec1, hour2, min2, sec2, gmt) {\nif (typeof hour1 != 'number' || typeof min1 != 'number' || typeof sec1 != 'number' \n|| hour1 < 0 || min1 < 0 || sec1 < 0 \n|| typeof hour2 != 'number' || typeof min2 != 'number' || typeof sec2 != 'number' \n|| hour2 < 0 || min2 < 0 || sec2 < 0 \n|| typeof gmt != 'boolean')  return false; \nvar cur = new Date();\nvar d1 = new Date();\nvar d2 = new Date();\nd1.setHours(hour1);\nd1.setMinutes(min1);\nd1.setSeconds(sec1);\nd2.setHours(hour2);\nd2.setMinutes(min2);\nd2.setSeconds(sec2);\nif (gmt == true)\ncur = new Date(cur.getTime() - cur.getTimezoneOffset() * 60 * 1000);\nreturn ((d1.getTime() <= cur.getTime()) && (cur.getTime() <= d2.getTime()));\n}");
		this.autoProxyScript.append(
				"function weekdayRange(wd1, wd2, gmt){\nif (typeof wd1 == 'undefined') \nreturn false;\nelse if (typeof wd2 == 'undefined' || _isGmt(wd2)) \nreturn _weekdayRange(wd1, wd1, _isGmt(wd2)); \nelse \nreturn _weekdayRange(wd1, wd2, _isGmt(gmt)); }\nfunction _weekdayRange(wd1, wd2, gmt) {\nif (typeof wd1 != 'string' || typeof wd2 != 'string' || typeof gmt != 'boolean') return false; \nvar w1 = -1, w2 = -1;\nfor (var i=0; i < _day.length; i++) {\nif (_day[i] == wd1)\nw1 = i;\nif (_day[i] == wd2)\nw2 = i; }\nvar cur = new Date();\nif (gmt == true)\ncur = new Date(cur.getTime() - cur.getTimezoneOffset() * 60 * 1000);\nvar w3 = cur.getDay();\nif (w1 > w2)\nw2 = w2 + 7;\nif (w1 > w3)\nw3 = w3 + 7;\nreturn (w1 <= w3 && w3 <= w2); }");
		this.autoProxyScript.append(" function alert() {} ");
		log.info("Executing builtin PAC functions");
		engine.eval(this.autoProxyScript.toString());
		ScriptEngineFactory sef = engine.getFactory();
		System.out.println(sef.getMethodCallSyntax("obj", "dnsResolve", "string"));
		engine.eval("obj.dnsResolve('')");
		log.info("Done executing builtin PAC functions");
	}

	public String dnsResolve(String paramString) {
		String str = "";
		try {
			str = InetAddress.getByName(paramString).getHostAddress();
		} catch (UnknownHostException localUnknownHostException) {
			localUnknownHostException.printStackTrace();
		}
		return str;
	}
}
