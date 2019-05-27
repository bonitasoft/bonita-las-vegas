
function getCookie(w){
	cName = "";
	pCOOKIES = new Array();
	pCOOKIES = document.cookie.split('; ');
	for(bb = 0; bb < pCOOKIES.length; bb++){
		NmeVal  = new Array();
		NmeVal  = pCOOKIES[bb].split('=');
		if(NmeVal[0] == w){
			cName = unescape(NmeVal[1]);
		}
	}
	return cName;
}

function printCookies(w){
	cStr = "";
	pCOOKIES = new Array();
	pCOOKIES = document.cookie.split('; ');
	for(bb = 0; bb < pCOOKIES.length; bb++){
		NmeVal  = new Array();
		NmeVal  = pCOOKIES[bb].split('=');
		if(NmeVal[0]){
			cStr += NmeVal[0] + '=' + unescape(NmeVal[1]) + '; ';
		}
	}
	return cStr;
}

function setCookie(name, value, expires, path, domain, secure){
	cookieStr = name + "=" + escape(value) + "; ";
	
	if(expires){
		expires = setExpiration(expires);
		cookieStr += "expires=" + expires + "; ";
	}
	if(path){
		cookieStr += "path=" + path + "; ";
	}
	if(domain){
		cookieStr += "domain=" + domain + "; ";
	}
	if(secure){
		cookieStr += "secure; ";
	}
	
	document.cookie = cookieStr;
}

function setExpiration(cookieLife){
    var today = new Date();
    var expr = new Date(today.getTime() + cookieLife * 24 * 60 * 60 * 1000);
    return  expr.toGMTString();
}


var setLanguage = function(sel){    
    var value = sel.value;  
    setCookie('BOS_Locale', '', -1, '/bonita/apps/showroom/landingCS/');
    setCookie('BOS_Locale', '', -1, '/bonita/');
    //setCookie('BOS_Locale', '', -1, '/');

    setCookie('BOS_Locale', value, 3, '/bonita/portal');
    setCookie('BOS_Locale', value, 3, '/');
    location.reload();
};

$(document).ready(function(){
	setCookie('BOS_Locale', '', -1, '/bonita/apps/showroom/landingCS/');

	var c = function(){
		return getCookie('BOS_Locale');		
	};

	$("#languageSelector").val(c);
});

