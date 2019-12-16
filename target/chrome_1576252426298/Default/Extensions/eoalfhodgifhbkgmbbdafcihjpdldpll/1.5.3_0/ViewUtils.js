function ViewUtils() { // jshint ignore: line
    // the css generator needs to be unique so different future uses of viewutils don't clash with each other

    var uid = Math.round( Math.random() * 1000) + "" + Math.round( Math.random() * 1000);
    if (typeof CssGenerator !== 'undefined') { //for the background script part
        var css = new CssGenerator("viewUtils"+uid, true);
        css.init();
    }

    function clipToElement(selector, element){
        var params = [element.offsetTop, element.offsetLeft+element.offsetWidth, element.offsetTop+element.offsetHeight, element.offsetLeft];
        if(extGlobal.browserGap.isSafari) {
            params = [0, element.offsetLeft+element.offsetWidth, element.offsetTop+element.offsetHeight, element.offsetLeft];
        }
        var clip = { clip: "rect("+params.join("px,")+")" };
        css.addSelector(selector, clip);
    }

    function clearInnerHTML(element){
        while (element.hasChildNodes()){
            element.removeChild(element.firstChild);
        }
    }

    function hideElement(element){
        element.classList.add("displayNone");
    }

    function unhideElement(element){
        element.classList.remove("displayNone");
    }

    /*
        resizeNewTab will center the page elements upon resize, in particular top sites
        It is position:absolute where we only show what is available for the user screen.
        Each topSite has a fixed padding left+right=30px, topSites that can't be shown are going to a 2nd line which is
        under the viewport (not visible to the user).
    */
    function resizeNewTab() {
        var topSites = document.querySelectorAll("#topSites div.topSite"),
            topSitesContainer = document.getElementById("topSites"),
            rightPanel = document.getElementById("rightPanel"),
            lastVisibleElement = 0,
            lastRightPos,
            clientWidth,
            availableRight,
            availableLeft,
            leftPaddingForCenter,
            dropupMenu = document.getElementById("dropupMenu"),
            tosPolicy = document.getElementById("tosPolicy");
        if (topSites.length > 0) {
            topSitesContainer.style.padding = '0 30px 0 30px'; //before resizing we reinit padding to 30px left and right
            for (var i = 0; i < topSites.length && isElementVisible(topSites[i]); i++) {
                lastVisibleElement = i;
            }

            lastRightPos = topSites[lastVisibleElement].offsetLeft + topSites[lastVisibleElement].offsetWidth;
            clientWidth = document.documentElement.clientWidth;
            availableRight = clientWidth - lastRightPos;
            if (rightPanel && !rightPanel.classList.contains("hideOnRight")) { //centering the top sites when we have a right panel
                topSitesContainer.classList.add("withRightPanel"); //this class is used in the css stylesheet
                availableRight = availableRight - rightPanel.offsetWidth;

                dropupMenu.style.marginRight = rightPanel.offsetWidth + 10 + "px"; //adding 10 because of the recent blur effect which is larger than the right panel

                if (tosPolicy) {
                    tosPolicy.style.marginRight = rightPanel.offsetWidth + 10 + "px";
                }
            } else {
                dropupMenu.style.marginRight = "0px";

                if (tosPolicy) {
                    tosPolicy.style.marginRight = "0px";
                }
            }

            availableLeft = topSites[0].offsetLeft;
            leftPaddingForCenter = Math.round((availableLeft + availableRight) / 2);
            topSitesContainer.style.padding = '0 30px 0 ' + leftPaddingForCenter + 'px';
        }
        return lastVisibleElement;
    }

    function isElementVisible(el) {
        var rect     = el.getBoundingClientRect(),
            vWidth   = window.innerWidth || document.documentElement.clientWidth,
            vHeight  = window.innerHeight || document.documentElement.clientHeight,
            efp      = function (x, y) { return document.elementFromPoint(x, y); };
        // Return false if it's not in the viewport - when window is too smal the icons will fall to next line which is below viewport (last condition)
        if ((rect.right < 0 || rect.bottom < 0) || rect.left > vWidth || rect.top > vHeight) {
            return false;
        } else {
            return true;
        }
    }


    function getAolSearchUrl(queryString, distribChannel) {
        if (distribChannel.aolParams) {
            return "https://search.aol.com/aol/search?q=" + encodeURIComponent(queryString) + distribChannel.aolParams;
        } else {
            return "https://search.aol.com/aol/search?q=" + encodeURIComponent(queryString) + "&s_it=aolbrowser-hyplogusaolc00000002&type=unifiedextn"; //default
        }
    }

    function getFFSearchUrl(queryString, distribChannel) {
        var typeParam = distribChannel.typeParam ? distribChannel.typeParam : extGlobal.constants.typeParam;
        return "https://" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_domain") +
                    (distribChannel.hsimp ? "/yhs" : "") +
                    "/" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_path") +
                    "?p=" + encodeURIComponent(queryString) +
                    (distribChannel.hspart ? "&hspart=" + distribChannel.hspart : "") +
                    (distribChannel.hsimp ? "&hsimp=" + distribChannel.hsimp : "") +
                    (distribChannel.frCodeFirefox && !distribChannel.hsimp ? "&fr=" + distribChannel.frCodeFirefox : "") +
                    "&type=" + typeParam;
    }

    function getSFSearchUrl(queryString, distribChannel) {
        var typeParam = distribChannel.typeParam ? distribChannel.typeParam : extGlobal.constants.typeParam;
        var searchParamSf = distribChannel.searchType === "fr" ? "&fr=" + distribChannel.frCodeSafari : "&hspart=" + distribChannel.hspart + "&hsimp=" + distribChannel.hsimp;
        return "https://" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_domain") +
            (distribChannel.searchType === "hsimp" ? "/yhs" : "") +
            "/" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_path") +
            "?p=" + encodeURIComponent(queryString) +
            searchParamSf + //for chrome it may be fr code OR hsimp+hspart, depending on config
            "&type=" + typeParam;
    }



    function getChrSearchUrl(queryString, distribChannel, isWebRequest) {
        var searchParam = distribChannel.searchType === "fr" ? "&fr=" + distribChannel.frCodeChrome : "&hspart=" + distribChannel.hspart + "&hsimp=" + distribChannel.hsimp;
        var typeDefault = distribChannel.typeDefault ? distribChannel.typeDefault : extGlobal.constants.typeDefault;
        var typeParam = distribChannel.typeParam ? distribChannel.typeParam : extGlobal.constants.typeParam;
        var url = "https://" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_domain") +
                    (distribChannel.searchType === "hsimp" ? "/yhs" : "") +
                    "/" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_path") +
                    "?p=" + encodeURIComponent(queryString) + searchParam;

        var type = "";
        var cmpgParam = "";
        var campaign = extGlobal.browserGap.localStorage.getItem("campaign");

        if (campaign && campaign.indexOf(extGlobal.constants.installDateTag) > -1) { //date handling
            var installTimestamp = extGlobal.browserGap.localStorage.getItem("firstRunCompletedTime");
            campaign = campaign.replace(extGlobal.constants.installDateTag, getInstallDateTag(installTimestamp));
        }

        if (distribChannel.subCampaigns && distribChannel.cmpgParam && campaign && campaign.indexOf("_") > -1) {
            cmpgParam = "&" + distribChannel.cmpgParam + "=" + encodeURIComponent(campaign.split("_")[1]) + "&type=" + encodeURIComponent(campaign.split("_")[0]);
        } else if (distribChannel.subCampaigns && campaign) { //if no particular cmpgParam, we use type
            cmpgParam = "&type=" + encodeURIComponent(campaign);
        }

        if (cmpgParam.indexOf("&type=") === -1) {
            type = "&type=" + (isWebRequest ? typeDefault : typeParam);
        }
        url += cmpgParam + type;
        return url;
    }

    function getSearchUrl(queryString, isWebRequest){
        var url = "",
            distribChannel = extGlobal.constants.distributionChannels[extGlobal.distributionChannel],
            isFirefox = extGlobal.browserGap.isFirefox || extGlobal.browserGap.isWebExtension,
            isSafari = extGlobal.browserGap.isSafari;
        if (extGlobal.constants.aolUI) {
            url = getAolSearchUrl(queryString, distribChannel);
        } else {
            if (isFirefox){
                url = getFFSearchUrl(queryString, distribChannel);
            }
            else if (isSafari){
                url = getSFSearchUrl(queryString, distribChannel);
            }
            else {
                url = getChrSearchUrl(queryString, distribChannel, isWebRequest);
            }
        }
        return url;
    }

    // TODO: need fr2 code and urls on FF and partners
    function getTrendingNowUrl(queryString){
        if (!queryString) {
            return "";
        }

        var distribChannel = extGlobal.constants.distributionChannels[extGlobal.distributionChannel],
            type = distribChannel.trendingNow && distribChannel.trendingNow.type ? distribChannel.trendingNow.type : "",
            url = "",
            tnFrCode = extGlobal.constants.tnFrCode;

        url = "https://" + extGlobal.browserGap.getLocalizedString("newtab_extension_search_prov_domain") +
            "/search" +
            "?p=" + encodeURIComponent(queryString) +
            "&fr=" + tnFrCode +
            (type ? "&type=" + type : "");

        return url;
    }

    function setTnViewParams (newTabData, beaconParams) {
        var enableTN = newTabData.enableTN || false;
        var tnData = newTabData.trendingNowData || null;
        var tnItems;

        if (newTabData.breakingNews) {
            beaconParams.tn_enable = extGlobal.constants.tn_enable_value; //breaking news
            beaconParams.aid = newTabData.breakingNews.uuid;
            var uniqueId = getBNUniqueId(newTabData.breakingNews);
            var shownTimes = JSON.parse(localStorage.getItem("bnAlreadyShown") || "{}");
            if (shownTimes[uniqueId]) {
                beaconParams.cat = "" + shownTimes[uniqueId];
            }

        } else if (enableTN) {
            beaconParams.tn_enable = "1";

            if (tnData) {
                tnData = JSON.parse(tnData);
                tnItems = tnData.items || [];
                beaconParams.tn_num = (tnItems.length || 0).toString();
            } else {
                beaconParams.tn_num = "0";
            }
        } else {
            beaconParams.tn_enable = "0";
            beaconParams.tn_num = "0";
        }
    }

    function getBNUniqueId(breakingNews) {
        return breakingNews ? (breakingNews.uuid + "_" + breakingNews.published_time) : "";
    }

    function setPartnerSiteFlag (beaconParams) {
        var partnerSites = localStorage.getItem("partnerSites") || "";
        beaconParams.partner_sites = partnerSites;
    }

    function setActivePartnerSites (partners, blackList) {
        var partnerSites = [],
            isBlacklist;
        if (extGlobal.browserGap.isChrome) {
            Object.keys(partners).forEach(function (key) {
                isBlacklist = false;
                for (var i = 0; i < blackList.length; i++) {
                    if (blackList[i] === partners[key].url) {
                        isBlacklist = true;
                    }
                }
                if (!isBlacklist) {
                    partnerSites.push(partners[key].title);
                }
            });
            localStorage.setItem("partnerSites", partnerSites.join(","));
        }
    }

    function stripHtml(str) {
        var tmp = document.implementation.createHTMLDocument().body;
        tmp.innerHTML = str;
        return tmp.textContent || tmp.innerText || "";
    }


    // Used both by content script and background script
    function getInstallDateTag(installTimestamp) {
        var installDate = "";
        try {
            if (installTimestamp && !isNaN(parseInt(installTimestamp))) {
                installDate = new Date(parseInt(installTimestamp));
                installDate = "" + installDate.getFullYear() +
                            ((installDate.getMonth()+1) < 10 ? "0" + (installDate.getMonth()+1) : installDate.getMonth()+1) +
                            (installDate.getDate() < 10 ? "0" + installDate.getDate() : installDate.getDate());
            }
        } catch (e) {

        }
        return installDate;
    }

    // Used by Chrome background script
    function chromeCheckPartner(sites) {
        // checkPartner function can be used both by chrome.tabs.query site list and also chrome.history.search
        var partnerFound = false;
        var partner;
        for (var i = 0; sites && i < sites.length; i++) {
            try {
                if (sites[i].url.indexOf("#extInstall?") > -1 && !partnerFound) {
                    var params = sites[i].url.substring(sites[i].url.indexOf("#extInstall?") + 12, sites[i].url.length);
                    partner = "external-" + params.split("&")[0].split("=")[1];
                } else if (sites[i].url.indexOf("extInstall=1") > -1 && !partnerFound) {
                    partner = "external-" + extractParam("partner", sites[i].url);
                }
                if (extGlobal.constants.distributionChannels[partner]) {
                    partnerFound = true;
                    if (extGlobal.constants.distributionChannels[partner].subCampaigns) {
                        var campaign = extractParam("campaign", sites[i].url);
                        extGlobal.browserGap.localStorage.setItem("campaign", campaign);
                    }
                    break;
                }
            } catch(e) {

            }
        }
        return partnerFound && partner ? partner : null;
    }

    function extractParam(param, url) {
        param ? param = param.replace(/[\[]/,"\\[").replace(/[\]]/,"\\]") : null;
        var p = (new RegExp("[\\?&]"+param+"=([^&#]*)")).exec(url);
        return (p===null) ? "" : p[1];
    }

    this.hideElement = hideElement;
    this.unhideElement = unhideElement;
    this.clearInnerHTML = clearInnerHTML;
    this.clipToElement = clipToElement;
    this.resizeNewTab = resizeNewTab;
    this.getSearchUrl = getSearchUrl;
    this.getTrendingNowUrl = getTrendingNowUrl;
    this.setTnViewParams = setTnViewParams;
    this.setPartnerSiteFlag = setPartnerSiteFlag;
    this.setActivePartnerSites = setActivePartnerSites;
    this.stripHtml = stripHtml;
    this.getBNUniqueId = getBNUniqueId;
    this.getInstallDateTag = getInstallDateTag;
    this.chromeCheckPartner = chromeCheckPartner;
    this.extractParam = extractParam;
    this.getAolSearchUrl = getAolSearchUrl;
    this.getChrSearchUrl = getChrSearchUrl;
    return this;
}
