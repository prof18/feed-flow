package com.prof18.feedflow.shared

val opml = """
    <?xml version="1.0" encoding="UTF-8"?>
    <opml version="1.0">
        <head>
            <title>Marco subscriptions in feedly Cloud</title>
        </head>
        <body>
            <outline text="Tech" title="Tech">
                <outline type="rss" text="Hacker News" title="Hacker News" xmlUrl="https://news.ycombinator.com/rss" htmlUrl="https://news.ycombinator.com/"/>
                <outline type="rss" text="Android Police - Feed" title="Android Police - Feed" xmlUrl="http://www.androidpolice.com/feed/" htmlUrl="https://www.androidpolice.com"/>
                <outline type="rss" text="TechCrunch" title="TechCrunch" xmlUrl="https://techcrunch.com/feed/" htmlUrl="https://techcrunch.com/"/>
            </outline>
            <outline text="Basket" title="Basket">
                <outline type="rss" text="Pianeta Basket" title="Pianeta Basket" xmlUrl="http://www.pianetabasket.com/rss/" htmlUrl="https://www.pianetabasket.com"/>
                <outline type="rss" text="Overtime" title="Overtime" xmlUrl="https://www.overtimebasket.com/feed/" htmlUrl="https://www.overtimebasket.com"/>
            </outline>
            <outline text="News" title="News">
                <outline type="rss" text="Il Post" title="Il Post" xmlUrl="http://feeds.ilpost.it/ilpost" htmlUrl="https://www.ilpost.it"/>
            </outline>
        </body>
    </opml>
""".trimIndent()

val opmlWithMalformedXml = """
    <?xml version=1.0 encoding=UTF-8?>
    <opml version="1.0">
        <head>
            <title>Test malformed OPML</title>
        </head>
        <body>
            <outline text="Tech" title="Tech">
                <outline type="rss" text="Test & Demo" title="Test & Demo" xmlUrl="https://test.com/rss" htmlUrl="https://test.com"/>
                <outline type="rss" text="Unclosed tag example" title="Unclosed tag example" xmlUrl="https://test2.com/rss" htmlUrl="https://test2.com"/>
                <outline type="rss" text="Special chars test" title="Special chars test" xmlUrl="https://test3.com/rss" htmlUrl="https://test3.com"/>
            </outline>
        </body>
    </opml>
""".trimIndent()

val opmlWithText = """
    <?xml version="1.0" encoding="UTF-8"?>
    <opml xmlns:rssowl="http://www.rssowl.org" version="1.1">
      <head>
        <title>RSSOwl Subscriptions</title>
        <dateModified>dim., 01 août 2021 08:40:15 CEST</dateModified>
      </head>
      <body>
        <outline text="My Feeds" rssowl:isSet="true" rssowl:id="7">
          <outline text="Subscription from Feed Subscriptions" rssowl:isSet="false" rssowl:id="36334">
            <outline text="Subscription from opml 13 janvier 2017.opml" rssowl:isSet="false" rssowl:id="36335">
              <outline text="My Feeds" rssowl:isSet="false" rssowl:id="36336">
                <outline text="androïd" rssowl:isSet="false" rssowl:id="36338">
                  <outline text="AnandTech" xmlUrl="https://www.anandtech.com/rss" rssowl:id="36340" />
                  <outline text="Android 6.0 Marshmallow â FrAndroid" xmlUrl="https://www.frandroid.com/feed" rssowl:id="36341" />
                  <outline text="Android Authority" xmlUrl="http://feed.androidauthority.com/" rssowl:id="36343" />
                  <outline text="Android Central - Android Forums, News, Reviews, Help and Android Wallpapers" xmlUrl="https://www.androidcentral.com/rss.xml" rssowl:id="36345" />
                  <outline text="Android et Vous" xmlUrl="http://www.androidetvous.com/feed" rssowl:id="36346" />
                  <outline text="Android Giveaway of the Day" xmlUrl="https://android.giveawayoftheday.com/feed" rssowl:id="36347" />
                  <outline text="Android MT" xmlUrl="https://www.android-mt.com/feed" rssowl:id="36348" />
                  <outline text="Android Police - Android News, Apps, Games, Phones, Tablets" xmlUrl="http://feeds.feedburner.com/AndroidPolice" rssowl:id="36349" />
                  <outline text="Android-Logiciels.fr" xmlUrl="http://www.android-logiciels.fr/feed/?post_type=listing_type" rssowl:id="36350" />
                  <outline text="Android-Zone" xmlUrl="http://www.android-zone.fr/feed" rssowl:id="36352" />
                  <outline text="AndroidXDA" xmlUrl="http://feeds.feedburner.com/AndroidXDA" rssowl:id="36351" />
                  <outline text="Begeek.fr" xmlUrl="http://www.begeek.fr/feed" rssowl:id="36356" />
                  <outline text="BGR India" xmlUrl="https://www.bgr.in/feed" rssowl:id="36358" />
                  <outline text="CNET Android Update" xmlUrl="https://www.cnet.com/rss/android-update" rssowl:id="36361" />
                  <outline text="CyanogenMods" xmlUrl="https://www.cyanogenmods.org/feed/" rssowl:id="36363" />
                  <outline text="Developpez.com" xmlUrl="https://www.developpez.com/index/rss" rssowl:id="36365" />
                  <outline text="Developpez.com Android" xmlUrl="https://android.developpez.com/index/rss" rssowl:id="36366" />
                  <outline text="Developpez.com Mobiles" xmlUrl="https://mobiles.developpez.com/index/rss" rssowl:id="36367" />
                  <outline text="Digital Trends" xmlUrl="https://www.digitaltrends.com/feed" rssowl:id="36370" />
                  <outline text="DigitBin" xmlUrl="https://www.digitbin.com/feed" rssowl:id="36371" />
                  <outline text="DroidSoft" xmlUrl="http://droidsoft.fr/feed" rssowl:id="36372" />
                  <outline text="DroidViews" xmlUrl="https://www.droidviews.com/feed" rssowl:id="36373" />
                  <outline text="Engadget RSS Feed" xmlUrl="http://www.engadget.com/rss-full.xml" rssowl:id="36374" />
                  <outline text="F-Droid - Free and Open Source Android App Repository" xmlUrl="https://f-droid.org/fr/feed.xml" rssowl:id="36376" />
                  <outline text="factory reset" xmlUrl="https://www.factoryreset.net/samsung-galaxy-j6-hard-reset/feed" rssowl:id="1797488" />
                  <outline text="FrAndroid" xmlUrl="http://feedpress.me/frandroid" rssowl:id="36378" />
                  <outline text="FrAndroid Â» Applications Android" xmlUrl="https://www.frandroid.com/android/applications/feed" rssowl:id="36379" />
                  <outline text="Freewares &amp; Tutos" xmlUrl="http://feeds2.feedburner.com/FreewaresTutos" rssowl:id="36381" />
                  <outline text="Freewares &amp; Tutos (Atom 1.0)" xmlUrl="http://feeds.feedburner.com/FreewaresTutos" rssowl:id="36382" />
                  <outline text="Google Lat Long" xmlUrl="http://maps.googleblog.com/feeds/posts/default" rssowl:id="36384" />
                  <outline text="Gotta Be Mobile" xmlUrl="http://feeds.feedburner.com/Gottabemobile" rssowl:id="36385" />
                  <outline text="GSMArena.com - Latest articles" xmlUrl="https://www.gsmarena.com/rss-news-reviews.php3" rssowl:id="36386" />
                  <outline text="HT Pratique" xmlUrl="https://htpratique.com/feed/" rssowl:id="36389" />
                  <outline text="IDBOOX" xmlUrl="http://www.idboox.com/feed" rssowl:id="36390" />
                  <outline text="JoyofAndroid.com" xmlUrl="https://joyofandroid.com/feed" rssowl:id="36392" />
                  <outline text="L'actualitÃ© des TIC au Maroc et ailleurs" xmlUrl="https://www.tic-maroc.com/feeds/posts/default?alt=rss" rssowl:id="36393" />
                  <outline text="Le Journal du Geek Â» Android" xmlUrl="https://www.journaldugeek.com/tag/android/feed" rssowl:id="36396" />
                  <outline text="Les Numériques" xmlUrl="https://www.lesnumeriques.com/rss.xml" rssowl:id="36397" />
                  <outline text="Les Numériques" xmlUrl="https://www.lesnumeriques.com/rss-tests-articles.xml" rssowl:id="36398" />
                  <outline text="LesMobiles.com" xmlUrl="https://www.lesmobiles.com/rss.xml" rssowl:id="36399" />
                  <outline text="LineageOS ROMs" xmlUrl="https://lineageosroms.com/feed" rssowl:id="2074953" />
                  <outline text="LineageOS ROMs [UNOFFICIAL]" xmlUrl="https://www.cyanogenmods.org/feed" rssowl:id="36400" />
                  <outline text="LTE â SlashGear" xmlUrl="https://www.slashgear.com/tags/lte/feed" rssowl:id="36401" />
                  <outline text="MiniMachines.net" xmlUrl="http://feedpress.me/minimachines" rssowl:id="36406" />
                  <outline text="MOBILE - Download Free Movies Games MP3 Albums and Softwares!" xmlUrl="http://www.downduck.com/mobile-and-ios/rss.xml" rssowl:id="36408" />
                  <outline text="Montre cardio GPS : tests, avis, comparaisons, news, rumeurs" xmlUrl="https://www.montre-cardio-gps.fr/feed" rssowl:id="36410" />
                  <outline text="NaldoTech" xmlUrl="https://www.naldotech.com/feed" rssowl:id="2040566" />
                  <outline text="NextPit" xmlUrl="https://www.nextpit.fr/feed/main.xml" rssowl:id="1656202" />
                  <outline text="NZ Tech Podcast" xmlUrl="http://nztp1.libsyn.com/rss" rssowl:id="36414" />
                  <outline text="Phandroid" xmlUrl="https://phandroid.com/feed" rssowl:id="36416" />
                  <outline text="PhoneArena - News" xmlUrl="https://www.phonearena.com/feed/news" rssowl:id="36417" />
                  <outline text="PhoneArena - Tablets - Phones" xmlUrl="https://www.phonearena.com/feed/tablets/new-phones" rssowl:id="36418" />
                  <outline text="PhoneArena - Tablets - Reviews" xmlUrl="https://www.phonearena.com/feed/tablets/reviews" rssowl:id="36419" />
                  <outline text="PixelExperience Blog" xmlUrl="https://blog.pixelexperience.org/feed" rssowl:id="2016872" />
                  <outline text="Pocket-lint" xmlUrl="https://www.pocket-lint.com/rss/all.xml" rssowl:id="36421" />
                  <outline text="Pocket-lint : Latest Reviews" xmlUrl="https://www.pocket-lint.com/rss/reviews/all.xml" rssowl:id="36422" />
                  <outline text="Prodigemobile" xmlUrl="http://www.prodigemobile.com/feed" rssowl:id="36423" />
                  <outline text="RealityGaming" xmlUrl="https://realitygaming.fr/forums/-/index.rss" rssowl:id="36424" />
                  <outline text="Root My Galaxy" xmlUrl="https://rootmygalaxy.net/feed" rssowl:id="36426" />
                  <outline text="SamMobile" xmlUrl="https://www.sammobile.com/feed" rssowl:id="36427" />
                  <outline text="Samsung Newsroom" xmlUrl="https://news.samsung.com/global/feed" rssowl:id="36429" />
                  <outline text="SlashGear" xmlUrl="https://www.slashgear.com/feed" rssowl:id="36430" />
                  <outline text="tablette Android" xmlUrl="https://www.lesandroides.net/feed" rssowl:id="36433" />
                  <outline text="Tablette-Tactile.net" xmlUrl="https://www.tablette-tactile.net/feed" rssowl:id="36434" />
                  <outline text="Techbargains.com" xmlUrl="https://www.techbargains.com/rss.xml" rssowl:id="36435" />
                  <outline text="TechDroider" xmlUrl="https://www.techdroider.com/feeds/posts/default?alt=rss" rssowl:id="36436" />
                  <outline text="TechSpot Reviews and Features" xmlUrl="https://www.techspot.com/reviews.xml" rssowl:id="36438" />
                  <outline text="tests d'applications AndroidPIT" xmlUrl="https://www.androidpit.fr/feed/main.xml" rssowl:id="36439" />
                  <outline text="TheAndroidPortal" xmlUrl="https://www.theandroidportal.com/feed" rssowl:id="36441" />
                  <outline text="TrickyDroid" xmlUrl="https://www.trickydroid.com/feed" rssowl:id="1692165" />
                  <outline text="TrustedReviews - Site-wide feed" xmlUrl="http://www.trustedreviews.com/feeds" rssowl:id="36446" />
                  <outline text="Tutoriel Android â Prodigemobile" xmlUrl="http://www.prodigemobile.com/category/tutoriel/feed" rssowl:id="36451" />
                  <outline text="Virus and Malware Removal" xmlUrl="https://www.techspot.com/community/forums/virus-and-malware-removal.28/index.rss" rssowl:id="36460" />
                  <outline text="Wccftech" xmlUrl="https://wccftech.com/feed" rssowl:id="36463" />
                  <outline text="WeAreMobians" xmlUrl="http://wearemobians.com/feed" rssowl:id="36465" />
                  <outline text="www.kulturechronik.fr Blog Feed" xmlUrl="https://www.kulturechronik.fr/rss/blog" rssowl:id="36467" />
                  <outline text="xda-developers" xmlUrl="https://www.xda-developers.com/feed" rssowl:id="2171419" />
                  <outline text="getdroid" xmlUrl="https://www.getdroidtips.com/feed" rssowl:id="2363509" />
                  <outline text="getdroid" xmlUrl="https://www.getdroidtips.com/feed" rssowl:id="2363510" />
                  <outline text="LineageOS" xmlUrl="https://lineageos.org/feed.xml" rssowl:id="2396223" />
                  <outline text="LineageOS Engineering Blog" xmlUrl="https://lineageos.org/feed-engineering.xml" rssowl:id="2396264" />
                  <outline text="OpenGApps" xmlUrl="https://sourceforge.net/projects/opengapps/rss?path=" rssowl:id="2463627" />
                  <outline text="Activity for OpenGApps" xmlUrl="https://sourceforge.net/p/opengapps/activity/feed" rssowl:id="2463629" />
                  <outline text="OpenGApps" xmlUrl="http://mix.chimpfeedr.com/5b110-OpenGApps" rssowl:id="2471237" />
                  <outline text="android activity" xmlUrl="https://gitlab.com/LineageOS/issues/android.atom" rssowl:id="2484232" />
                  <outline text="lineage klte" xmlUrl="https://www.lineageoslog.com/rss/18.1/klte" rssowl:id="2540434" />
                  <outline text="OpenGApps" xmlUrl="http://mix.chimpfeedr.com/5b110-OpenGApps" rssowl:id="2548460" />
                  <outline text="OpenGApps" xmlUrl="http://mix.chimpfeedr.com/5b110-OpenGApps" rssowl:id="2548461" />
                  <outline text="line" xmlUrl="https://www.lineageoslog.com/rss/18.1/klte" rssowl:id="2563033" />
                </outline>
                <outline text="défense" rssowl:isSet="false" rssowl:id="36470">
                  <outline text="3rd-wing" xmlUrl="https://www.3rd-wing.net/index.php?act=rssout&amp;id=1" rssowl:id="36473" />
                  <outline text="45eNord.ca - ActualitÃ©s militaires, dÃ©fense, technologie, armÃ©e, marine, aviation" xmlUrl="http://www.45enord.ca/feed" rssowl:id="36474" />
                  <outline text="AAE" xmlUrl="https://academieairespace.com/feed/" rssowl:id="36475" />
                  <outline text="Actu et Conseils - Trading Sat" xmlUrl="https://www.tradingsat.com/rssfeed.php" rssowl:id="36476" />
                  <outline text="actumarine" xmlUrl="https://actumaritime.com/feed/" rssowl:id="36478" />
                  <outline text="ActuNautique.com" xmlUrl="http://www.actunautique.com/rss" rssowl:id="36479" />
                  <outline text="Aeronewstv" xmlUrl="http://www.aeronewstv.com/fr/?p=flux_rss&amp;id=1&amp;type=rss" rssowl:id="36482" />
                  <outline text="Aerospatium" xmlUrl="https://www.aerospatium.info/feed" rssowl:id="36485" />
                  <outline text="Agence nationale de la sÃ©curitÃ© des systÃ¨mes d'information" xmlUrl="https://www.ssi.gouv.fr/feed/actualite" rssowl:id="36486" />
                  <outline text="AICPRAT" xmlUrl="http://www.aicprat.fr/feed" rssowl:id="637695" />
                  <outline text="Air et Cosmos" xmlUrl="http://www.air-cosmos.com/rss/air-cosmos.xml" rssowl:id="36489" />
                  <outline text="AssemblÃ©e nationale" xmlUrl="http://www2.assemblee-nationale.fr/feeds/detail/crs" rssowl:id="36496" />
                  <outline text="AssemblÃ©e nationale" xmlUrl="http://www2.assemblee-nationale.fr/feeds/detail/documents-parlementaires" rssowl:id="36497" />
                  <outline text="AssemblÃ©e nationale" xmlUrl="http://www2.assemblee-nationale.fr/feeds/detail/ID_420120/(type)/instance" rssowl:id="36498" />
                  <outline text="AssemblÃ©e nationale" xmlUrl="http://www2.assemblee-nationale.fr/feeds/detail/ID_59046/(type)/instance" rssowl:id="36499" />
                  <outline text="AssemblÃ©e nationale" xmlUrl="http://www2.assemblee-nationale.fr/feeds/detail/ID_59048/(type)/instance" rssowl:id="36500" />
                  <outline text="Australian Manufacturing" xmlUrl="http://www.australianmanufacturing.com.au/feed" rssowl:id="36502" />
                  <outline text="Aviation and Space - Download PDF magazines -  Magazines Commumity!" xmlUrl="https://downmagaz.com/aviation_magazine_space/rss.xml" rssowl:id="36504" />
                  <outline text="Aviation and Space Magazines - Download PDF magazines - French Magazines Commumity!" xmlUrl="https://fr.downmagaz.com/aviation_magazine_space_francaise/rss.xml" rssowl:id="36505" />
                  <outline text="AVIATIONNEWS.EU" xmlUrl="http://aviationnews.eu/feed" rssowl:id="36507" />
                  <outline text="avionslegendaires.net" xmlUrl="https://www.avionslegendaires.net/feed" rssowl:id="36509" />
                  <outline text="Breaking Defense" xmlUrl="http://feeds.feedburner.com/BreakingDefense" rssowl:id="36511" />
                  <outline text="brest" xmlUrl="https://brest.maville.com/flux/rss/actu.php?c=loc&amp;code=br&amp;dep=29" rssowl:id="36512" />
                  <outline text="Bruxelles2" xmlUrl="https://www.bruxelles2.eu/feed/" rssowl:id="36513" />
                  <outline text="Cadre et Dirigeant Magazine" xmlUrl="https://www.cadre-dirigeant-magazine.com/feed" rssowl:id="36514" />
                  <outline text="capital.fr" xmlUrl="https://www.capital.fr/rss" rssowl:id="36515" />
                  <outline text="Centre Thucydide" xmlUrl="https://www.afri-ct.org/feed/" rssowl:id="36517" />
                  <outline text="CFDT THALES Avionics le Haillan" xmlUrl="http://cfdtthavhai.canalblog.com/rss.xml" rssowl:id="36518" />
                  <outline text="CGT THALES AIR SYSTEMS" xmlUrl="http://tr6.cgtthales.fr/feed" rssowl:id="36521" />
                  <outline text="cherbourg" xmlUrl="https://cherbourg.maville.com/flux/rss/actu.php?c=loc&amp;code=ch&amp;dep=50" rssowl:id="36522" />
                  <outline text="China Defense Blog" xmlUrl="http://china-defense.blogspot.com/atom.xml" rssowl:id="36524" />
                  <outline text="Claude Arpi" xmlUrl="http://claudearpi.blogspot.com/feeds/posts/default" rssowl:id="36527" />
                  <outline text="cols bleus" xmlUrl="https://www.colsbleus.fr/full-articles-feed.xml" rssowl:id="36528" />
                  <outline text="cols bleus mensuel" xmlUrl="https://www.colsbleus.fr/magazines_mensuel_feed" rssowl:id="36529" />
                  <outline text="defense industry daily" xmlUrl="http://feeds.feedburner.com/did/rss" rssowl:id="36541" />
                  <outline text="Defense of the Republic of the Philippines" xmlUrl="http://defenseph.net/drp/index.php?PHPSESSID=4tsas28hdik8sivfcah085l530&amp;type=rss;action=.xml" rssowl:id="36542" />
                  <outline text="Defense One - All Content" xmlUrl="http://www.defenseone.com/rss/all" rssowl:id="36543" />
                  <outline text="DEFENSE STUDIES" xmlUrl="http://defense-studies.blogspot.com/feeds/posts/default?alt=rss" rssowl:id="36544" />
                  <outline text="Defense-Update" xmlUrl="https://defense-update.com/feed" rssowl:id="36546" />
                  <outline text="Digital Battlespace" xmlUrl="https://www.shephardmedia.com/news/digital-battlespace/feed/" rssowl:id="36547" />
                  <outline text="DÃ©fense globale" xmlUrl="http://defense.blogs.lavoixdunord.fr/index.rss" rssowl:id="36535" />
                  <outline text="Econostrum | Toute l'actualité économique en Méditerranée" xmlUrl="https://www.econostrum.info/xml/syndication.rss" rssowl:id="36552" />
                  <outline text="ElectronicsB2B" xmlUrl="https://www.electronicsb2b.com/feed" rssowl:id="36553" />
                  <outline text="Enderi" xmlUrl="https://www.enderi.fr/xml/syndication.rss" rssowl:id="36554" />
                  <outline text="Escadrilles.org - Nouvelles du site" xmlUrl="https://feeds.feedburner.com/escadrilles" rssowl:id="36555" />
                  <outline text="Europartenaires" xmlUrl="http://www.europartenaires.net/feed" rssowl:id="36557" />
                  <outline text="FOB - Forces Operations Blog" xmlUrl="http://forcesoperations.com/feed" rssowl:id="36559" />
                  <outline text="fondation Bordeaux UniversitÃ©" xmlUrl="http://www.fondation.univ-bordeaux.fr/feed" rssowl:id="36560" />
                  <outline text="Geopragma" xmlUrl="http://geopragma.fr/index.php/feed" rssowl:id="36565" />
                  <outline text="Hobby Magazines | Download Free Digital Magazines And Books" xmlUrl="http://www.hobbymagazines.org/rss.xml" rssowl:id="36568" />
                  <outline text="Intelligence Online : Dernier NumÃ©ro" xmlUrl="http://feeds.feedburner.com/IntelligenceOnline-fr" rssowl:id="36573" />
                  <outline text="INVESTIGATION OCEANOGRAPHIQUE ET OANIS" xmlUrl="https://investigationsoanisetoceanographiee.com/feed" rssowl:id="36575" />
                  <outline text="IPSA" xmlUrl="http://feeds2.feedburner.com/ionis/ipsa" rssowl:id="36576" />
                  <outline text="l'Association ENSTA ParisTech Alumni" xmlUrl="https://www.ensta.org/global/rss.php" rssowl:id="36581" />
                  <outline text="L'histoire en rafale" xmlUrl="http://lhistoireenrafale.lunion.fr/feed" rssowl:id="36592" />
                  <outline text="La Chaire Ãconomie de dÃ©fense" xmlUrl="http://economie-defense.fr/feed" rssowl:id="36579" />
                  <outline text="Le blog d'ISD" xmlUrl="http://www.isd.sorbonneonu.fr/blog/feed/" rssowl:id="36582" />
                  <outline text="Le mamouth" xmlUrl="http://lemamouth.blogspot.com/feeds/posts/default?alt=rss" rssowl:id="36585" />
                  <outline text="Le Point - DÃ©fense ouverte" xmlUrl="https://www.lepoint.fr/editos-du-point/jean-guisnel/rss.xml" rssowl:id="36586" />
                  <outline text="Les Echos - actualitÃ© aÃ©ronautique" xmlUrl="http://syndication.lesechos.fr/rss/rss_aero.xml" rssowl:id="36590" />
                  <outline text="Lignes de dÃ©fense" xmlUrl="http://lignesdedefense.blogs.ouest-france.fr/atom.xml" rssowl:id="36594" />
                  <outline text="Liste des billets" xmlUrl="https://www.irsem.fr/institut/actualites/rss/posts.html" rssowl:id="36596" />
                  <outline text="lorient" xmlUrl="https://lorient.maville.com/flux/rss/actu.php?c=loc&amp;code=lo&amp;dep=56" rssowl:id="36598" />
                  <outline text="Mars attaque" xmlUrl="http://mars-attaque.blogspot.com/feeds/posts/default" rssowl:id="36602" />
                  <outline text="mer et marine" xmlUrl="https://www.meretmarine.com/fr/meretmarinerss.xml" rssowl:id="36604" />
                  <outline text="Meta-Defense.fr" xmlUrl="https://www.meta-defense.fr/feed/" rssowl:id="36606" />
                  <outline text="Mil-Log" xmlUrl="https://www.shephardmedia.com/news/mil-log/feed" rssowl:id="36609" />
                  <outline text="Military - WorldMags free digital magazine download your desktop | Oron.com, FilePost.com - Downloads Digital Magazines PDF &amp; True PDF magazines, electronic journal, Read Free new magazines for the iPad WorldMags.net" xmlUrl="https://worldmags.net/military/rss.xml" rssowl:id="36607" />
                  <outline text="Military-Today.com" xmlUrl="http://www.military-today.com/rss.xml" rssowl:id="36608" />
                  <outline text="Missilethreat.com" xmlUrl="http://missilethreat.csis.org/feed" rssowl:id="36610" />
                  <outline text="ModÃ¨les de charges externes" xmlUrl="https://www.aviationsmilitaires.net/v2/base/rss/externalloadmodel.xml" rssowl:id="36611" />
                  <outline text="MÃ¡quina de Combate" xmlUrl="http://maquina-de-combate.com/blog/?feed=rss2" rssowl:id="36599" />
                  <outline text="Naval News" xmlUrl="https://www.navalnews.com/feed/" rssowl:id="36616" />
                  <outline text="Naval Today" xmlUrl="http://feeds.feedburner.com/Navaltoday" rssowl:id="36619" />
                  <outline text="Northrop Grumman" xmlUrl="http://news.northropgrumman.com/rss.xml" rssowl:id="36620" />
                  <outline text="Observatoire des multinationales" xmlUrl="http://multinationales.org/spip.php?page=backend" rssowl:id="36621" />
                  <outline text="PACE" xmlUrl="https://pacetoday.com.au/feed" rssowl:id="36623" />
                  <outline text="Pakistan Defence 1" xmlUrl="http://defence.pk/forums/-/index.rss" rssowl:id="36624" />
                  <outline text="Aéronautique &amp; Défense" xmlUrl="https://www.latribune.fr/entreprises-finance/industrie/aeronautique-defense/feed.xml" rssowl:id="1732796" />
                  <outline text="Parlons Aviation" xmlUrl="https://www.parlonsaviation.com/feed/" rssowl:id="36625" />
                  <outline text="Pax Aquitania" xmlUrl="http://www.paxaquitania.fr/feeds/posts/default?alt=rss" rssowl:id="36626" />
                  <outline text="portail des sous marins" xmlUrl="https://www.corlobe.tk/index.php?page=backend" rssowl:id="36628" />
                  <outline text="Presse" xmlUrl="https://www.thalesgroup.com/fr/feeds/press-room/rss.xml" rssowl:id="36629" />
                  <outline text="RAFALE : The omnirole fighter" xmlUrl="http://omnirole-rafale.com/feed" rssowl:id="36631" />
                  <outline text="Revista IngenierÃ­a Naval" xmlUrl="https://sectormaritimo.es/feed" rssowl:id="36632" />
                  <outline text="RIA Novosti" xmlUrl="https://fr.sputniknews.com/export/rss2/index.xml" rssowl:id="36633" />
                  <outline text="Save the Royal Navy" xmlUrl="https://www.savetheroyalnavy.org/feed" rssowl:id="36635" />
                  <outline text="secrets-de-la-guerre-froide - Derniers articles" xmlUrl="https://static.blog4ever.com/2018/03/843013/rss_articles.xml" rssowl:id="36636" />
                  <outline text="SEDE - Parlement européen" xmlUrl="http://www.europarl.europa.eu/rss/committee/sede/fr.xml" rssowl:id="36637" />
                  <outline text="south front" xmlUrl="https://southfront.org/feed/" rssowl:id="36642" />
                  <outline text="SpÃ©cial DÃ©fense" xmlUrl="http://specialdefense.over-blog.com/rss" rssowl:id="36643" />
                  <outline text="Submarine Matters" xmlUrl="http://gentleseas.blogspot.com/feeds/posts/default" rssowl:id="36645" />
                  <outline text="Submarine Matters" xmlUrl="http://gentleseas.blogspot.com/feeds/posts/default?alt=rss" rssowl:id="36646" />
                  <outline text="thales" xmlUrl="https://www.thalesgroup.com/en/feeds/press-room/rss.xml" rssowl:id="36649" />
                  <outline text="The Atlantic" xmlUrl="http://feeds.feedburner.com/TheAtlantic" rssowl:id="36654" />
                  <outline text="The Times of IsraÃ«l" xmlUrl="http://fr.timesofisrael.com/feed" rssowl:id="36655" />
                  <outline text="Theatrum Belli" xmlUrl="https://www.theatrum-belli.com/feed" rssowl:id="36656" />
                  <outline text="theengineer" xmlUrl="https://www.theengineer.co.uk/feed" rssowl:id="36657" />
                  <outline text="TV83" xmlUrl="http://www.tv83.info/feed" rssowl:id="36661" />
                  <outline text="USNI News" xmlUrl="https://news.usni.org/feed" rssowl:id="36666" />
                  <outline text="VIPress.net" xmlUrl="https://www.vipress.net/feed/" rssowl:id="36668" />
                  <outline text="Word of the Day" xmlUrl="http://www.thefreedictionary.com/_/WoD/rss.aspx" rssowl:id="36670" />
                  <outline text="World Airline News" xmlUrl="https://worldairlinenews.com/feed" rssowl:id="36671" />
                  <outline text="Zone Militaire" xmlUrl="http://feeds2.feedburner.com/ZoneMilitaire" rssowl:id="36672" />
                  <outline text="APDR" xmlUrl="https://asiapacificdefencereporter.com/feed" rssowl:id="1425738" />
                  <outline text="OGlobo" xmlUrl="https://oglobo.globo.com/rss.xml?secao=pais" rssowl:id="2268808" />
                  <outline text="Le blog d'Ivan Rioufol" xmlUrl="https://blogrioufol.com/feed" rssowl:id="1612607" />
                  <outline text="Flux RSS Toutes les actualités" xmlUrl="https://www.economie.gouv.fr/rss/toutesactualites" rssowl:id="1732817" />
                  <outline text="EchoRadar.eu" xmlUrl="https://echoradar.eu/feed" rssowl:id="1842292" />
                  <outline text="Le Fauteuil de Colbert" xmlUrl="https://lefauteuildecolbert.blogspot.com/feeds/posts/default?alt=rss" rssowl:id="1872743" />
                  <outline text="Red Samovar" xmlUrl="https://redsamovar.com/feed" rssowl:id="2040482" />
                  <outline text="Opinions Libres" xmlUrl="https://www.oezratty.net/wordpress/feed" rssowl:id="2284795" />
                </outline>
                <outline text="el pais" rssowl:isSet="false" rssowl:id="36673">
                  <outline text="IntercambiosVirtuales" xmlUrl="http://feeds.feedburner.com/jimmy_criptoy" rssowl:id="36674" />
                </outline>
                <outline text="espagnol" rssowl:isSet="false" rssowl:id="36676" />
                <outline text="Internet" rssowl:isSet="false" rssowl:id="36682">
                  <outline text="[CANAL Assistance] Derniers sujets" xmlUrl="https://assistance.canal.fr/xml/questions" rssowl:id="36684" />
                  <outline text="[CANAL Assistance] Dernières réponses" xmlUrl="https://assistance.canal.fr/xml/answers" rssowl:id="36683" />
                  <outline text="[Molotov] DerniÃ¨res questions" xmlUrl="http://aide.molotov.tv/xml/questions" rssowl:id="36685" />
                  <outline text="Activity for PdfBooklet" xmlUrl="https://sourceforge.net/p/pdfbooklet/activity/feed" rssowl:id="36687" />
                  <outline text="Actualités logiciel" xmlUrl="https://www.lemondeinformatique.fr/flux-rss/thematique/logiciel/rss.xml" rssowl:id="36688" />
                  <outline text="Actualités mobilite" xmlUrl="https://www.lemondeinformatique.fr/flux-rss/mobilite/rss.xml" rssowl:id="36689" />
                  <outline text="Agence nationale de la sÃ©curitÃ© des systÃ¨mes d'information" xmlUrl="http://www.ssi.gouv.fr/feed/guide" rssowl:id="36690" />
                  <outline text="Agence nationale de la sÃ©curitÃ© des systÃ¨mes d'information" xmlUrl="http://www.ssi.gouv.fr/feed/publication" rssowl:id="36691" />
                  <outline text="Alain Bensoussan" xmlUrl="https://www.alain-bensoussan.com/feed" rssowl:id="36693" />
                  <outline text="An RSS Blog - Daily News and Information Related to RSS Feeds, Syndication and Aggregation." xmlUrl="http://www.rss-specifications.com/blog-feed.xml" rssowl:id="36694" />
                  <outline text="Anti-Malware Zone" xmlUrl="https://nicolascoolman.eu/feed" rssowl:id="36695" />
                  <outline text="Applis, Logiciels - 01net" xmlUrl="https://www.01net.com/rss/actualites/applis-logiciels/" rssowl:id="36697" />
                  <outline text="Ars Technica" xmlUrl="http://feeds.arstechnica.com/arstechnica/index" rssowl:id="1444639" />
                  <outline text="Assistance Free" xmlUrl="http://assistance.free.fr/news/index.xml" rssowl:id="36699" />
                  <outline text="Assistance Free" xmlUrl="https://assistance.free.fr/news/index.xml" rssowl:id="36700" />
                  <outline text="Assistance Free" xmlUrl="https://assistance-1.free.fr/news/index.xml" rssowl:id="1907767" />
                  <outline text="BetaNews.Com #1" xmlUrl="https://betanews.com/feed" rssowl:id="36702" />
                  <outline text="BOX Actualité" xmlUrl="https://www.ariase.com/fr/news/ariase_rss.xml" rssowl:id="1272060" />
                  <outline text="Bugtracker Freebox ::  Freebox Server" xmlUrl="https://dev.freebox.fr/bugs/feed.php?feed_type=rss1&amp;project=9" rssowl:id="36703" />
                  <outline text="Bugtracker Freebox :: Application &quot;Freebox Connect&quot;" xmlUrl="https://dev.freebox.fr/bugs/feed.php?feed_type=rss2&amp;project=16" rssowl:id="36704" />
                  <outline text="Bugtracker Freebox :: Application Freebox" xmlUrl="https://dev.freebox.fr/bugs/feed.php?feed_type=rss2&amp;project=11" rssowl:id="36705" />
                  <outline text="Bugtracker Freebox :: Freebox Player Delta / One (V7)" xmlUrl="https://dev.freebox.fr/bugs/feed.php?feed_type=rss1&amp;project=13" rssowl:id="36706" />
                  <outline text="Bugtracker Freebox :: Freebox Player Pop (V8)" xmlUrl="https://dev.freebox.fr/bugs/feed.php?feed_type=rss2&amp;project=14" rssowl:id="36707" />
                  <outline text="Catégorie Windows - Tous les threads, Windows 10, Performances et maintenance, Ordinateur" xmlUrl="https://answers.microsoft.com/fr-fr/feed/f/windows/windows_10-performance-winpc?tab=Threads&amp;status=all&amp;threadType=All" rssowl:id="36710" />
                  <outline text="Cccam For All" xmlUrl="https://cccam4all.net/feed/" rssowl:id="36842" />
                  <outline text="clubic com actualitÃ©s" xmlUrl="https://www.clubic.com/articles.rss" rssowl:id="36712" />
                  <outline text="CommentCaMarche.net" xmlUrl="https://www.commentcamarche.net/rss" rssowl:id="36716" />
                  <outline text="CrackingPatching" xmlUrl="https://crackingpatching.com/feed" rssowl:id="1842439" />
                  <outline text="Data Security Breach" xmlUrl="https://www.datasecuritybreach.fr/feed" rssowl:id="36718" />
                  <outline text="Derniers Banc d'essai Articles de Tech Advisor" xmlUrl="https://www.techadvisor.fr/banc-essai/rss" rssowl:id="36719" />
                  <outline text="diaporama du jour" xmlUrl="https://www.diaporamas-a-la-con.com/fluxRss/FluxRss.xml" rssowl:id="36720" />
                  <outline text="Digital Inspiration Technology Blog" xmlUrl="http://feeds.labnol.org/labnol" rssowl:id="36721" />
                  <outline text="Downloadcrew -" xmlUrl="https://www.downloadcrew.com/feeds/rss/recommended.php" rssowl:id="36723" />
                  <outline text="Downloadcrew - Latest Software" xmlUrl="https://www.downloadcrew.com/feeds/rss/latest.php" rssowl:id="36724" />
                  <outline text="Downloadcrew - News" xmlUrl="https://www.downloadcrew.com/feeds/rss/news" rssowl:id="594514" />
                  <outline text="easy tutorial" xmlUrl="https://www.easytutoriel.com/feed" rssowl:id="1280961" />
                  <outline text="Flux RSS de Canard PC" xmlUrl="https://www.canardpc.com/feed" rssowl:id="36728" />
                  <outline text="Flux toutes les actualités - 01net" xmlUrl="https://www.01net.com/rss/info/flux-rss/flux-toutes-les-actualites/" rssowl:id="36729" />
                  <outline text="Forum 60 millions de consommateurs" xmlUrl="https://www.60millions-mag.com/forum/feed.php" rssowl:id="36730" />
                  <outline text="foumrep windows" xmlUrl="https://www.tenforums.com/external.php?type=RSS2" rssowl:id="1075401" />
                  <outline text="framalog" xmlUrl="https://framablog.org/feed" rssowl:id="36734" />
                  <outline text="Framasoft - Toute l'actualitÃ©" xmlUrl="https://rss.framasoft.org/" rssowl:id="36735" />
                  <outline text="Fredzone" xmlUrl="http://www.fredzone.org/feed" rssowl:id="36736" />
                  <outline text="Freenews" xmlUrl="https://www.freenews.fr/feed" rssowl:id="36738" />
                  <outline text="freewarefiles" xmlUrl="http://www.freewarefiles.com/rss/newfiles.xml" rssowl:id="36740" />
                  <outline text="FunInformatique" xmlUrl="https://www.funinformatique.com/feed" rssowl:id="36741" />
                  <outline text="generation-nt" xmlUrl="http://www.generation-nt.com/export/rss.xml" rssowl:id="36743" />
                  <outline text="gHacks Technology News" xmlUrl="https://www.ghacks.net/feed/" rssowl:id="36744" />
                  <outline text="gratilog 2" xmlUrl="http://www.gratilog.net/xoops/modules/rss/rss.php?feed=mydownloads" rssowl:id="36745" />
                  <outline text="Gratilog, rien que des freewares !" xmlUrl="http://www.gratilog.net/xoops/modules/rss/rss.php" rssowl:id="36746" />
                  <outline text="Green IT" xmlUrl="http://feeds.feedburner.com/GreenIT" rssowl:id="36747" />
                  <outline text="Haloule.com: les combines qui dégomment" xmlUrl="https://www.haloule.com/feeds/posts/default" rssowl:id="1944290" />
                  <outline text="Have I Been Pwned latest breaches" xmlUrl="https://feeds.feedburner.com/HaveIBeenPwnedLatestBreaches" rssowl:id="2003671" />
                  <outline text="How-To Geek" xmlUrl="https://feeds.howtogeek.com/HowToGeek" rssowl:id="36749" />
                  <outline text="HT Pratique" xmlUrl="https://htpratique.com/feed" rssowl:id="1842837" />
                  <outline text="infologiciel" xmlUrl="https://www.bluenote-systems.com/feed" rssowl:id="36750" />
                  <outline text="Korben" xmlUrl="https://korben.info/feed" rssowl:id="36751" />
                  <outline text="La Fibre" xmlUrl="https://lafibre.info/.xml/?type=rss;PHPSESSID=2c717bs9adrongjlt3pgsbqi27" rssowl:id="36752" />
                  <outline text="La Toile de Busyspider Aide abonnes Free Alice" xmlUrl="https://www.busyspider.fr/rss.xml" rssowl:id="36753" />
                  <outline text="Lagazette.fr Â» Toute l'actualitÃ©" xmlUrl="https://www.lagazettedescommunes.com/rubriques/actualite/feed" rssowl:id="36755" />
                  <outline text="Le Blog de Back Market" xmlUrl="https://story.backmarket.fr/feed" rssowl:id="1594194" />
                  <outline text="Le blog de l'actualitÃ© d'Univers Freebox" xmlUrl="https://lebloguniversfreebox.actuly.fr/feed" rssowl:id="36756" />
                  <outline text="Le blog de libellules.ch" xmlUrl="http://feeds2.feedburner.com/LeBlogDeLibellulesch?feed/rss2" rssowl:id="36757" />
                  <outline text="Le Crabe Info" xmlUrl="https://lecrabeinfo.net/feed" rssowl:id="36758" />
                  <outline text="Le forum des portables Asus" xmlUrl="https://www.forum-des-portables-asus.fr/forums/forums/-/index.rss" rssowl:id="36759" />
                  <outline text="Le Saviez Vous ?" xmlUrl="https://saviezvous.actuly.fr/feed/" rssowl:id="36760" />
                  <outline text="Ma PlanÃ¨te PPS / DIAPORAMA gratuit a telecharger :: Blogues" xmlUrl="http://ma-planete.com/public/rss/act_blogs/rss_20" rssowl:id="36766" />
                  <outline text="MajorGeeks.com #1" xmlUrl="https://www.majorgeeks.com/backend.php?id=120" rssowl:id="36768" />
                  <outline text="malekal's site" xmlUrl="https://www.malekal.com/feed" rssowl:id="36770" />
                  <outline text="mos poular" xmlUrl="https://www.downloadcrew.com/feeds/rss/popular.php" rssowl:id="36771" />
                  <outline text="neonet" xmlUrl="http://neo-net.fr/forum/feed.php" rssowl:id="36772" />
                  <outline text="neonet" xmlUrl="http://neo-net.fr/forum/feed.php?mode=topics" rssowl:id="36775" />
                  <outline text="News Freebox V8" xmlUrl="https://freeboxv8.actuly.fr/feed/" rssowl:id="36778" />
                  <outline text="NewsGuard" xmlUrl="https://www.newsguardtech.com/feed" rssowl:id="887386" />
                  <outline text="NirSoft - Freeware Utilities" xmlUrl="http://www.nirsoft.net/rss-new.xml" rssowl:id="36780" />
                  <outline text="OBJETCONNECTE.NET" xmlUrl="https://www.objetconnecte.net/feed" rssowl:id="36783" />
                  <outline text="PC Astuces : Astuces" xmlUrl="http://feeds2.feedburner.com/PcAstucesAstuces" rssowl:id="36784" />
                  <outline text="PDF.co" xmlUrl="https://pdf.co/feed/" rssowl:id="36786" />
                  <outline text="PhonAndroid" xmlUrl="https://www.phonandroid.com/feed" rssowl:id="36787" />
                  <outline text="Portable4PC" xmlUrl="https://portable4pc.com/feed/" rssowl:id="36788" />
                  <outline text="PPS RSS Title" xmlUrl="http://ma-planete.com/public/rss/act_pps/rss_10" rssowl:id="36789" />
                  <outline text="presence pc internet" xmlUrl="https://www.tomshardware.fr/feed" rssowl:id="36790" />
                  <outline text="Presse-citron" xmlUrl="https://www.presse-citron.net/feed/" rssowl:id="36791" />
                  <outline text="Produits - 01net" xmlUrl="https://www.01net.com/rss/actualites/produits/" rssowl:id="36792" />
                  <outline text="rss-derniers-tests - 01net" xmlUrl="https://www.01net.com/rss/tests/les-derniers-tests/rss-derniers-tests/" rssowl:id="36794" />
                  <outline text="search engine" xmlUrl="http://feeds.searchengineland.com/searchengineland" rssowl:id="36795" />
                  <outline text="SecTools" xmlUrl="https://sectools.org/feed" rssowl:id="36796" />
                  <outline text="Siècle Digital" xmlUrl="https://siecledigital.fr/feed/" rssowl:id="36798" />
                  <outline text="Smartphones - 01net" xmlUrl="https://www.01net.com/rss/smartphones/" rssowl:id="36799" />
                  <outline text="SnapFiles latest software" xmlUrl="http://feeds.feedburner.com/snapfiles/latest" rssowl:id="36800" />
                  <outline text="sociÃ©tÃ©s" xmlUrl="https://www.zdnet.fr/feeds/rss/actualites" rssowl:id="36801" />
                  <outline text="Sospc" xmlUrl="https://sospc.name/feed" rssowl:id="36802" />
                  <outline text="Stylistme blog du digital marketing IOT formation" xmlUrl="https://stylistme.com/feed" rssowl:id="36806" />
                  <outline text="Techdows" xmlUrl="https://techdows.com/feed" rssowl:id="36809" />
                  <outline text="Techno-Science.net - Multimédia" xmlUrl="https://www.techno-science.net/include/news9.xml" rssowl:id="36812" />
                  <outline text="Technos - 01net" xmlUrl="https://www.01net.com/rss/actualites/technos/" rssowl:id="36811" />
                  <outline text="TechSpot" xmlUrl="https://www.techspot.com/backend.xml" rssowl:id="36813" />
                  <outline text="TechSpot Downloads" xmlUrl="https://www.techspot.com/downloads.xml" rssowl:id="36814" />
                  <outline text="TekRevue" xmlUrl="https://www.tekrevue.com/feed" rssowl:id="36815" />
                  <outline text="Test-Achats News feed" xmlUrl="https://www.test-achats.be/rss" rssowl:id="36816" />
                  <outline text="The Hacker News" xmlUrl="http://feeds.feedburner.com/TheHackersNews" rssowl:id="36817" />
                  <outline text="tous les drivers" xmlUrl="https://www.touslesdrivers.com/php/scripts/news_rss.php" rssowl:id="36820" />
                  <outline text="tout windows" xmlUrl="http://feeds2.feedburner.com/Toutwindows" rssowl:id="36822" />
                  <outline text="TÃ©lÃ© Satellite &amp; NumÃ©rique" xmlUrl="http://www.telesatellite.com/actu/rss.xml" rssowl:id="36808" />
                  <outline text="UnderNews" xmlUrl="http://feeds.feedburner.com/undernews/oCmA" rssowl:id="36824" />
                  <outline text="Univers Freebox l'intégrale" xmlUrl="https://universfreeboxlintegrale.actuly.fr/feed/" rssowl:id="36827" />
                  <outline text="Univers Freebox La Chaîne" xmlUrl="https://universfreeboxlachaine.actuly.fr/feed" rssowl:id="36825" />
                  <outline text="Usine Digitale L'Usine Digitale - ActualitÃ©s Ã  la une" xmlUrl="https://www.usine-digitale.fr/rss" rssowl:id="36828" />
                  <outline text="vulgarisateur" xmlUrl="https://vulgumtechus.com/index.php?title=Sp%C3%A9cial:Modifications_r%C3%A9centes&amp;feed=atom" rssowl:id="36829" />
                  <outline text="w10 reo" xmlUrl="https://www.tenforums.com/external.php?type=RSS2&amp;forumids=115" rssowl:id="1081064" />
                  <outline text="windows 10 rep" xmlUrl="https://www.tenforums.com/external.php?type=RSS2" rssowl:id="1079432" />
                  <outline text="Windows 10 â Windows 8 â Windows 7 â VISTA" xmlUrl="http://www.chantal11.com/feed" rssowl:id="36831" />
                  <outline text="Windows Central - News, Forums, Reviews, Help for Windows Phone" xmlUrl="http://feeds.feedburner.com/wmexperts" rssowl:id="36833" />
                  <outline text="Windows Latest" xmlUrl="https://www.windowslatest.com/feed/" rssowl:id="36834" />
                  <outline text="ZATAZ Alertes" xmlUrl="https://www.zataz.com/feed" rssowl:id="36835" />
                  <outline text="ZDNet - Business et Solutions IT" xmlUrl="https://www.zdnet.fr/feeds/rss/" rssowl:id="36836" />
                  <outline text="Zotero Documentation" xmlUrl="https://www.zotero.org/support/feed.php" rssowl:id="36840" />
                  <outline text="The Verge -  All Posts" xmlUrl="https://www.theverge.com/rss/index.xml" rssowl:id="2258882" />
                  <outline text="Siècle Digital" xmlUrl="https://siecledigital.fr/feed" rssowl:id="2615551" />
                </outline>
              </outline>
              <outline text="Subscription from FeedDemon Subscriptions" rssowl:isSet="false" rssowl:id="36843">
                <outline text="anglais" rssowl:isSet="false" rssowl:id="36844" />
                <outline text="Comics" rssowl:isSet="false" rssowl:id="36847" />
                <outline text="espagnol" rssowl:isSet="false" rssowl:id="36850">
                  <outline text="e Learn Spanish Language" xmlUrl="https://www.lawlessspanish.com/blog/feed" rssowl:id="36851" />
                  <outline text="eljueves.es - Últimas noticias" xmlUrl="https://www.eljueves.es/feeds/rss.html" rssowl:id="36852" />
                  <outline text="ELPAIS.com - Sección Internacional" xmlUrl="http://ep01.epimg.net/rss/internacional/portada.xml" rssowl:id="36853" />
                  <outline text="Free Libros" xmlUrl="http://feeds.feedburner.com/Ebookss?format=xml" rssowl:id="36854" />
                </outline>
                <outline text="français" rssowl:isSet="false" rssowl:id="36857" />
                <outline text="hugo" rssowl:isSet="false" rssowl:id="36859" />
                <outline text="My Feeds" rssowl:isSet="false" rssowl:id="36870" />
                <outline text="News" rssowl:isSet="false" rssowl:id="36874">
                  <outline text="1er site Film Streaming 100% Gratuit, Stream Complet VF HD/4K - Papystreaming" xmlUrl="https://papystreaming-hd.online/rss.xml" rssowl:id="1800244" />
                  <outline text="9Docu" xmlUrl="https://9docu.net/feed/" rssowl:id="36880" />
                  <outline text="Acrimed | Action Critique MÃ©dias" xmlUrl="http://www.acrimed.org/spip.php?page=backend" rssowl:id="36885" />
                  <outline text="AFP RSS" xmlUrl="https://www.afp.com/fr/actus/afp_actualite/792%2C31%2C9%2C7%2C33/feed" rssowl:id="1366171" />
                  <outline text="Agence Bretagne Presse" xmlUrl="https://abp.bzh/rss-all.php" rssowl:id="36886" />
                  <outline text="ANTICOR" xmlUrl="http://www.anticor.org/feed" rssowl:id="36888" />
                  <outline text="Atlantico.fr" xmlUrl="http://www.atlantico.fr/rss.xml" rssowl:id="36892" />
                  <outline text="Barbanews.com" xmlUrl="https://www.barbanews.com/feed" rssowl:id="1122737" />
                  <outline text="Bored Panda" xmlUrl="https://www.boredpanda.com/feed" rssowl:id="36900" />
                  <outline text="Cartoon | The Guardian" xmlUrl="https://www.theguardian.com/cartoons/archive/rss" rssowl:id="36902" />
                  <outline text="Chronik" xmlUrl="https://chronik.fr/feed" rssowl:id="36904" />
                  <outline text="Commentaires pour Le Vent Se LÃ¨ve" xmlUrl="http://lvsl.fr/comments/feed" rssowl:id="36908" />
                  <outline text="Commentaires pour SITE D INFORMATIONS" xmlUrl="https://ns2017.wordpress.com/comments/feed" rssowl:id="36909" />
                  <outline text="ComparatiFR" xmlUrl="https://comparatifr.com/feed" rssowl:id="2185805" />
                  <outline text="Computer - Download PDF magazines - Magazines Commumity!" xmlUrl="https://downmagaz.com/computer_magazine/rss.xml" rssowl:id="36910" />
                  <outline text="Controverses" xmlUrl="http://blog.lefigaro.fr/threard/atom.xml" rssowl:id="36914" />
                  <outline text="Courrier international - ActualitÃ©s France et Monde, cartoons, insolites" xmlUrl="https://www.courrierinternational.com/feed/all/rss.xml" rssowl:id="36915" />
                  <outline text="cyber" xmlUrl="https://veillecyberland.wordpress.com/feed" rssowl:id="36916" />
                  <outline text="Daily Geek Show" xmlUrl="https://dailygeekshow.com/feed" rssowl:id="36919" />
                  <outline text="Download Free Movies Games MP3 Albums and Softwares!" xmlUrl="http://www.downduck.com/rss.xml" rssowl:id="36923" />
                  <outline text="Download PDF magazines -  Magazines Commumity!" xmlUrl="https://downmagaz.com/rss.xml" rssowl:id="36924" />
                  <outline text="Download PDF magazines - French Magazines Commumity!" xmlUrl="https://fr.downmagaz.com/rss.xml" rssowl:id="36925" />
                  <outline text="Economie Matin" xmlUrl="http://www.economiematin.fr/flux/alaune.xml" rssowl:id="36929" />
                  <outline text="Edition Lyon - Villeurbanne : toutes les infos sur Le Progrès | Le Progrès" xmlUrl="https://www.leprogres.fr/edition-lyon-villeurbanne/rss" rssowl:id="1732571" />
                  <outline text="Eurolibertés" xmlUrl="https://eurolibertes.com/feed" rssowl:id="36932" />
                  <outline text="Europe" xmlUrl="https://www.reddit.com/r/europe/.rss" rssowl:id="36933" />
                  <outline text="Extreme Down (Extreme Download) - Téléchargement gratuits" xmlUrl="https://www.extreme-down.live/rss.xml" rssowl:id="1690595" />
                  <outline text="Fact Checker" xmlUrl="http://feeds.washingtonpost.com/rss/rss_fact-checker" rssowl:id="36937" />
                  <outline text="FactCheck.org" xmlUrl="https://www.factcheck.org/feed" rssowl:id="36938" />
                  <outline text="films" xmlUrl="https://www.astuces-aide-informatique.info/feed" rssowl:id="1505474" />
                  <outline text="films" xmlUrl="https://series-stream.tv/films/feed" rssowl:id="2185846" />
                  <outline text="Fils RSS du site FMI" xmlUrl="https://www.imf.org/en/news/rss?Language=FRA" rssowl:id="36951" />
                  <outline text="Flux RSS Fipeco" xmlUrl="https://www.fipeco.fr/flux_rss.xml" rssowl:id="36952" />
                  <outline text="France StratÃ©gie" xmlUrl="http://www.strategie.gouv.fr/rss.xml" rssowl:id="36955" />
                  <outline text="France Stratégie - Laboratoire d’idées public" xmlUrl="https://www.strategie.gouv.fr/rss.xml" rssowl:id="36956" />
                  <outline text="France Stratégie - Laboratoire d’idées public" xmlUrl="https://www.strategie.gouv.fr/rss.xml" rssowl:id="504834" />
                  <outline text="Francetv info - Europe" xmlUrl="https://www.francetvinfo.fr/monde/europe.rss" rssowl:id="36958" />
                  <outline text="Francetv info - Politique" xmlUrl="https://www.francetvinfo.fr/politique.rss" rssowl:id="36959" />
                  <outline text="Francetv info - Tendances" xmlUrl="https://www.francetvinfo.fr/economie/tendances.rss" rssowl:id="36960" />
                  <outline text="Francofolies - SudOuest.fr" xmlUrl="https://www.sudouest.fr/culture/francofolies/rss.xml" rssowl:id="387290" />
                  <outline text="French Anime - Animes VF et VOSTFR en Streaming Gratuit" xmlUrl="https://french-anime.com/rss.xml" rssowl:id="1800211" />
                  <outline text="FunInformatique" xmlUrl="https://www.funinformatique.com/feed/" rssowl:id="36963" />
                  <outline text="General" xmlUrl="https://www.ccomptes.fr/fr/rss/general" rssowl:id="36964" />
                  <outline text="HOAX-NET" xmlUrl="https://hoax-net.be/feed" rssowl:id="36969" />
                  <outline text="Hobbylit.net - daily updated collection magazines and book for download to PC, Mac, iOS &amp; Android" xmlUrl="https://www.hobbylit.net/rss.xml" rssowl:id="36970" />
                  <outline text="hongkiat.com" xmlUrl="http://feeds2.feedburner.com/24thfloor" rssowl:id="36971" />
                  <outline text="Insolentiae" xmlUrl="https://insolentiae.com/feed" rssowl:id="36974" />
                  <outline text="Jared Ranahan" xmlUrl="https://www.forbes.com/sites/jaredranahan/feed/" rssowl:id="36979" />
                  <outline text="Jeddl" xmlUrl="https://sugunamothersdelight.com/feed" rssowl:id="1800276" />
                  <outline text="JForum" xmlUrl="https://www.jforum.fr/feed" rssowl:id="36982" />
                  <outline text="journalb2b.com" xmlUrl="https://www.journalb2b.com/feed" rssowl:id="1926057" />
                  <outline text="Jurisdiction cour des comptes" xmlUrl="https://www.ccomptes.fr/fr/rss/juridiction/98" rssowl:id="36984" />
                  <outline text="JustGeek" xmlUrl="https://www.justgeek.fr/feed/" rssowl:id="36985" />
                  <outline text="Konbini France" xmlUrl="https://www.konbini.com/fr/feed" rssowl:id="36986" />
                  <outline text="L' Essentiel - SudOuest.fr" xmlUrl="https://www.sudouest.fr/essentiel/rss.xml" rssowl:id="36987" />
                  <outline text="L'actualitÃ© de Coe-Rexecode" xmlUrl="http://www.rexecode.fr/public/layout/set/rss/content/view/rss_public/2" rssowl:id="36993" />
                  <outline text="l'Opinion" xmlUrl="https://www.lopinion.fr/rss.xml" rssowl:id="37014" />
                  <outline text="La caverne de Pandoon" xmlUrl="https://pandoon.info/feed/" rssowl:id="36988" />
                  <outline text="La Conversation scientifique" xmlUrl="http://radiofrance-podcast.net/podcast09/rss_13957.xml" rssowl:id="36989" />
                  <outline text="La Croix.com - Les derniers articles : Toutes catégories" xmlUrl="https://www.la-croix.com/RSS/UNIVERS_ALL" rssowl:id="36990" />
                  <outline text="la tribune" xmlUrl="http://www.latribune.fr/rss/rubriques/actualite.html" rssowl:id="36992" />
                  <outline text="Le blog de Decitre : conseils, Ã©vÃ©nements, actualitÃ©s littÃ©raires" xmlUrl="https://www.decitre.fr/blog/rss" rssowl:id="36994" />
                  <outline text="Le blog des maisons de retraite" xmlUrl="https://www.retraiteplus.fr/pages/rss.html" rssowl:id="36995" />
                  <outline text="Le Colonel  ActualitÃ©s" xmlUrl="https://lecolonel.net/feed" rssowl:id="36996" />
                  <outline text="Le direct | www.cnews.fr" xmlUrl="http://fetchrss.com/rss/5d9f4c7d8a93f892718b45675e197ee98a93f81a218b4567.xml" rssowl:id="387257" />
                  <outline text="Le Huffington Post" xmlUrl="https://www.huffingtonpost.fr/feeds/index.xml" rssowl:id="36998" />
                  <outline text="Le Monde diplomatique" xmlUrl="http://www.monde-diplomatique.fr/recents.xml" rssowl:id="37000" />
                  <outline text="Le nouvel Economiste" xmlUrl="https://www.lenouveleconomiste.fr/feed" rssowl:id="1810803" />
                  <outline text="Le petit Shaman" xmlUrl="https://www.lepetitshaman.com/feed/" rssowl:id="37001" />
                  <outline text="Le Point - ActualitÃ©" xmlUrl="https://www.lepoint.fr/rss.xml" rssowl:id="37002" />
                  <outline text="Le Point - Politique" xmlUrl="https://www.lepoint.fr/politique/rss.xml" rssowl:id="37003" />
                  <outline text="Le Vent Se LÃ¨ve" xmlUrl="http://lvsl.fr/feed" rssowl:id="37004" />
                  <outline text="LeGossip.net" xmlUrl="https://www.legossip.net/feed" rssowl:id="37005" />
                  <outline text="Les Cahiers de la retraite complémentaire" xmlUrl="https://cahiers.laretraitecomplementaire.fr/rss.xml" rssowl:id="1352921" />
                  <outline text="Les-Crises.fr" xmlUrl="http://feeds.feedburner.com/les-crises-fr" rssowl:id="37010" />
                  <outline text="les4verites" xmlUrl="http://www.les4verites.com/feed" rssowl:id="37009" />
                  <outline text="Lexilogos" xmlUrl="https://www.lexilogos.com/rss.xml" rssowl:id="2234366" />
                  <outline text="LEXPRESS.fr - A la Une" xmlUrl="https://www.lexpress.fr/rss/alaune.xml" rssowl:id="37011" />
                  <outline text="Liberation - A la une sur LibÃ©ration" xmlUrl="http://rss.liberation.fr/rss/latest" rssowl:id="37012" />
                  <outline text="MAGAZINES - Download Free Movies Games MP3 Albums and Softwares!" xmlUrl="http://www.downduck.com/magazines/rss.xml" rssowl:id="37015" />
                  <outline text="MediaSportif" xmlUrl="https://www.mediasportif.fr/feed" rssowl:id="37018" />
                  <outline text="Military and Army Magazines - Download PDF magazines - French Magazines Commumity!" xmlUrl="https://fr.downmagaz.com/military_magazine_francaise/rss.xml" rssowl:id="37019" />
                  <outline text="Ministère des Solidarités et de la Santé" xmlUrl="https://solidarites-sante.gouv.fr/spip.php?page=backend" rssowl:id="37021" />
                  <outline text="Miss Penny Stocks" xmlUrl="http://misspennystocks.com/feed" rssowl:id="37023" />
                  <outline text="New Lien direct" xmlUrl="http://new.liens-direct.com/feed" rssowl:id="37027" />
                  <outline text="NYT &gt; Home Page" xmlUrl="http://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml" rssowl:id="37030" />
                  <outline text="Objectif Gard" xmlUrl="http://www.objectifgard.com/feed" rssowl:id="37031" />
                  <outline text="Ouest-France - Actualité" xmlUrl="https://www.ouest-france.fr/rss-en-continu.xml" rssowl:id="1865378" />
                  <outline text="Page d'accueil â The Conversation" xmlUrl="http://theconversation.com/fr/home-page/articles.atom" rssowl:id="37037" />
                  <outline text="PDF Gratuits" xmlUrl="http://pdfgratuits.blogspot.com/feeds/posts/default?alt=rss" rssowl:id="37040" />
                  <outline text="Pew Research Center's Global Attitudes Project" xmlUrl="https://www.pewresearch.org/global/feed" rssowl:id="2075053" />
                  <outline text="Planetoscope . com - La planète vivante ! - Statistiques écologiques en temps réel." xmlUrl="https://www.planetoscope.com/rss.php" rssowl:id="37043" />
                  <outline text="POLITICO - TOP Stories" xmlUrl="https://www.politico.com/rss/politicopicks.xml" rssowl:id="37044" />
                  <outline text="PowerPost" xmlUrl="http://feeds.washingtonpost.com/rss/rss_powerpost" rssowl:id="37046" />
                  <outline text="PrÃªchi-PrÃªcha" xmlUrl="http://www.prechi-precha.fr/feed" rssowl:id="37047" />
                  <outline text="Que Choisir.org" xmlUrl="https://www.quechoisir.org/utils/flux" rssowl:id="37049" />
                  <outline text="RePlay - VidÃ©os populaires" xmlUrl="https://www.replay.fr/all.rss" rssowl:id="37059" />
                  <outline text="Riposte Laique" xmlUrl="https://ripostelaique.com/feed" rssowl:id="37062" />
                  <outline text="rss canal u" xmlUrl="https://www.canal-u.tv/flux-rss-ballado/filter_rss.audio_video/format.rss?xts=248546&amp;xtor=RSS-1" rssowl:id="37063" />
                  <outline text="Site pour télécharger des films gratuitement" xmlUrl="https://9divx.theproxy.ws/b/?https://9divx.theproxy.ws/feed" rssowl:id="1842437" />
                  <outline text="Site pour télécharger des films gratuitement" xmlUrl="https://9divx.theproxy.ws/b/?https://9divx.theproxy.ws/feed" rssowl:id="2201812" />
                  <outline text="SooCurious" xmlUrl="http://feeds.feedburner.com/DailyGeekShow" rssowl:id="37074" />
                  <outline text="Sports, Outdoors &amp; Recreation - Your free magazines PDF" xmlUrl="http://www.magfree.net/sports-outdoors-recreation/rss.xml" rssowl:id="2220066" />
                  <outline text="Streaming Sport" xmlUrl="http://streaming-sport.tv/feed" rssowl:id="1926097" />
                  <outline text="Sud Ouest.fr" xmlUrl="https://www.sudouest.fr/rss.xml" rssowl:id="2003673" />
                  <outline text="SÃ©nat - communiquÃ©s de presse" xmlUrl="http://www.senat.fr/rss/presse.rss" rssowl:id="37065" />
                  <outline text="Tags AFP" xmlUrl="https://www.lepoint.fr/tags/rss/AFP.xml" rssowl:id="1366215" />
                  <outline text="Telecharger Des Magazines, Journaux et Livres Gratuitement" xmlUrl="https://telecharge-magazines.com/feed" rssowl:id="37085" />
                  <outline text="Telecharger Des Magazines, Journaux et Livres Gratuitement" xmlUrl="https://telecharge-magazines.com/feed" rssowl:id="1926095" />
                  <outline text="Telecharger livres bd comics mangas magazines" xmlUrl="http://zone-ebook.com/rss.xml" rssowl:id="37087" />
                  <outline text="The Guardian World News" xmlUrl="https://www.theguardian.com/uk/rss" rssowl:id="37091" />
                  <outline text="Tirexo - 1er site de téléchargement direct francophone" xmlUrl="https://www2.tirexo.club/rss.xml" rssowl:id="2171562" />
                  <outline text="Titrespresse.com - Flux RSS" xmlUrl="https://www.titrespresse.com/actualite.rss" rssowl:id="37097" />
                  <outline text="Touteleurope.eu : Tous les contenus" xmlUrl="https://www.touteleurope.eu/rss/tous-les-contenus.html" rssowl:id="37101" />
                  <outline text="Trucs et astuces malins" xmlUrl="https://www.comment-economiser.fr/rss-radin-malin.php" rssowl:id="389864" />
                  <outline text="True PDF Digital Magazine - Download free digital magazines for iPhone iPad, Android, Smartphone, PC and Mac device" xmlUrl="https://www.worldmags.net/rss.xml" rssowl:id="37102" />
                  <outline text="TV, Film &amp; Cinema - Your free magazines PDF" xmlUrl="http://www.magfree.net/tv-film-cinema/rss.xml" rssowl:id="37104" />
                  <outline text="TÃ©lÃ©charger SÃ©ries     VF  Gratuitement  - Zone TÃ©lÃ©chargement" xmlUrl="https://www2.tirexo.club/series-vf/?cstart=1/rss.xml" rssowl:id="2201814" />
                  <outline text="TÃ©lÃ©rama.fr - L'actu MÃ©dias / Net" xmlUrl="https://www.telerama.fr/rss/medias.xml" rssowl:id="37080" />
                  <outline text="varmatin.com" xmlUrl="https://www.varmatin.com/rss" rssowl:id="37108" />
                  <outline text="Vu du Droit" xmlUrl="http://www.vududroit.com/feed" rssowl:id="37118" />
                  <outline text="Wiflix - Film et Série streaming  en vf ou vostfr en HD" xmlUrl="https://wiflix.club/rss.xml" rssowl:id="1865346" />
                  <outline text="WorldMags free digital magazine download your desktop | Oron.com, FilePost.com - Downloads Digital Magazines PDF &amp; True PDF magazines, electronic journal, Read Free new magazines for the iPad WorldMags.net" xmlUrl="https://worldmags.net/rss.xml" rssowl:id="37122" />
                  <outline text="Xerfi Canal" xmlUrl="https://www.xerficanal.com/rss.xml" rssowl:id="37123" />
                  <outline text="Your free magazines PDF" xmlUrl="http://www.magfree.net/rss.xml" rssowl:id="37124" />
                  <outline text="Zone Streaming" xmlUrl="http://www.zone-streaming.fr/feed" rssowl:id="37125" />
                  <outline text="À la une - Google Actualités" xmlUrl="https://news.google.com/rss?hl=fr&amp;gl=FR&amp;ceid=FR%3Afr&amp;oc=11" rssowl:id="422309" />
                  <outline text="https://rss.elconfidencial.com/economia/EconomíaEconomía2021-06-26T05:11:02+02:00https://www.elconfidencial.com/img/logo/logo-el-confidencial.pngelconfidencial.comhttps://www.elconfidencial.comelconfidencial.comNo faltan camareros, faltan soldadores: este es el empleo que creará España este veranoLas que habitualmente protagonizan las contrataciones estivales, Baleares, Canarias, Barcelona, Madrid, son las que más lejos se quedan de volver a la normalidad 2021-06-26T05:00:00+02:002021-06-26T05:00:00+02:002021-06-26T05:00:00+02:00https://www.elconfidencial.com/economia/2021-06-26/no-faltan-camareros-faltan-soldadores-este-es-el-empleo-que-creara-espana-este-verano_3150168/Álvaro M. Setiénhttps://www.elconfidencial.com/autores/alvaro-m-setien-2822/El verano ya está aquí y con él, la habitual campaña masiva de contrataciones podría volver tras la pandemia. Sin embargo, aún no será como antes. El covid rompió con una racha cada vez mayor de nuevos contratos y este año, aunque vivirá un crecimiento del 20% respect" xmlUrl="https://rss.elconfidencial.com/economia" rssowl:id="2249766" />
                  <outline text="Spin-off.fr : Toute l'actualité et critiques des épisodes de vos séries TV préférées" xmlUrl="http://feeds.feedburner.com/spin-off-actu" rssowl:id="2249808" />
                  <outline text="Nouveaux Films - SokroStream - Voir Film Streaming 2021 gratuit. Film Complet en Stream VF" xmlUrl="https://sokrostream.tube/streaming-complet-4k/rss.xml" rssowl:id="2338713" />
                  <outline text="Zone Streaming" xmlUrl="http://www.zone-streaming.fr/feed" rssowl:id="2338726" />
                  <outline text="Streaming-Series.LA" xmlUrl="https://streaming-series.la/feed" rssowl:id="2388380" />
                  <outline text="serie" xmlUrl="http://video.stream-serie.cc/rss.xml" rssowl:id="2388422" />
                  <outline text="Les-docus.com" xmlUrl="https://www.les-docus.com/feed" rssowl:id="2388424" />
                  <outline text="Zone Streaming" xmlUrl="http://www.zone-streaming.fr/feed" rssowl:id="2643595" />
                  <outline text="Tirexo - 1er site de téléchargement direct francophone" xmlUrl="https://www2.tirexo.work/rss.xml" rssowl:id="2696652" />
                </outline>
                <outline text="santé" rssowl:isSet="false" rssowl:id="37138">
                  <outline text="cadredesante.com" xmlUrl="https://www.cadredesante.com/spip/spip.php?page=backend" rssowl:id="37142" />
                  <outline text="FHP-MCO" xmlUrl="https://www.fhpmco.fr/feed/" rssowl:id="37144" />
                  <outline text="Pratiques, les cahiers de la médecine utopique" xmlUrl="https://pratiques.fr/spip.php?page=backend" rssowl:id="37152" />
                  <outline text="Social / Santé - le fil d'actualité de service-public.fr" xmlUrl="https://www.service-public.fr/abonnements/rss/actu-th-social-sante.rss" rssowl:id="1944490" />
                  <outline text="Our World in Data" xmlUrl="https://ourworldindata.org/atom.xml" rssowl:id="2234372" />
                </outline>
                <outline text="sondages" rssowl:isSet="false" rssowl:id="37155" />
              </outline>
            </outline>
          </outline>
          <outline text="RSSOwl News" xmlUrl="http://www.rssowl.org/newsfeed" rssowl:id="1733421" />
          <outline text="BBC News" xmlUrl="http://news.bbc.co.uk/rss/newsonline_world_edition/front_page/rss091.xml" rssowl:id="1733422" />
          <outline text="New York Times" xmlUrl="http://feeds.nytimes.com/nyt/rss/HomePage" rssowl:id="1733425" />
          <outline text="Business" rssowl:isSet="false" rssowl:id="2017116">
            <outline text="Fast Company" xmlUrl="http://www.fastcompany.com/rss.xml" rssowl:id="2017117" />
            <outline text="I Will Teach You To Be Rich" xmlUrl="http://www.iwillteachyoutoberich.com/atom.xml" rssowl:id="2017118" />
            <outline text="Inc.com" xmlUrl="http://www.inc.com/rss.xml" rssowl:id="2017119" />
            <outline text="Law.com - Newswire" xmlUrl="http://www.law.com/rss/rss_newswire.xml" rssowl:id="2017120" />
            <outline text="Moneycontrol Top Headlines" xmlUrl="http://moneycontrol.com/rss/latestnews.xml" rssowl:id="2017122" />
            <outline text="NYT &gt; Business" xmlUrl="http://www.nytimes.com/services/xml/rss/nyt/Business.xml" rssowl:id="2017123" />
            <outline text="Smartmoney.com" xmlUrl="http://www.smartmoney.com/rss/smheadlines.cfm?feed=1&amp;format=rss091" rssowl:id="2017124" />
            <outline text="The Motley Fool" xmlUrl="http://www.fool.com/xml/foolnews_rss091.xml" rssowl:id="2017125" />
            <outline text="TheStreet.com" xmlUrl="http://www.thestreet.com/feeds/rss/index.xml" rssowl:id="2017126" />
            <outline text="WSJ.com: US Business" xmlUrl="http://online.wsj.com/xml/rss/0,,3_7014,00.xml" rssowl:id="2017127" />
          </outline>
          <outline text="Entertainment" rssowl:isSet="false" rssowl:id="2017169">
            <outline text="Art" rssowl:isSet="false" rssowl:id="2017170">
              <outline text="Art News Blog" xmlUrl="http://www.artnewsblog.com/atom.xml" rssowl:id="2017171" />
              <outline text="Drawn!" xmlUrl="http://drawn.ca/feed/" rssowl:id="2017173" />
              <outline text="lines and colors" xmlUrl="http://www.linesandcolors.com/feed/" rssowl:id="2017174" />
              <outline text="Modern Art Notes" xmlUrl="http://www.artsjournal.com/artsjournal1/atom.xml" rssowl:id="2017175" />
              <outline text="Rhizome Inclusive" xmlUrl="http://rhizome.org/syndicate/fp.rss" rssowl:id="2017176" />
              <outline text="VVORK" xmlUrl="http://www.vvork.com/?feed=rss2" rssowl:id="2017177" />
              <outline text="we make money not art" xmlUrl="http://feeds.we-make-money-not-art.com/wmmna" rssowl:id="2017178" />
            </outline>
            <outline text="Books" rssowl:isSet="false" rssowl:id="2017180">
              <outline text="Books News and Reviews" xmlUrl="http://www.guardian.co.uk/rssfeed/0,,10,00.xml" rssowl:id="2017181" />
              <outline text="London Review of Books" xmlUrl="http://www.lrb.co.uk/homerss.xml" rssowl:id="2017182" />
              <outline text="NPR Topics: Books" xmlUrl="http://www.npr.org/rss/rss.php?id=1032" rssowl:id="2017183" />
              <outline text="Salon: Books" xmlUrl="http://feeds.salon.com/salon/books" rssowl:id="2017185" />
              <outline text="The Literary Saloon" xmlUrl="http://www.complete-review.com/saloon/rss.xml" rssowl:id="2017186" />
              <outline text="The New York Review of Books" xmlUrl="http://feeds.feedburner.com/nybooks" rssowl:id="2017187" />
            </outline>
            <outline text="Comics" rssowl:isSet="false" rssowl:id="2017195">
              <outline text="Dilbert Daily Strip" xmlUrl="http://feeds.dilbert.com/DilbertDailyStrip" rssowl:id="2017196" />
              <outline text="Dinosaur Comics" xmlUrl="http://www.rsspect.com/rss/qwantz.xml" rssowl:id="2017197" />
              <outline text="PvPonline" xmlUrl="http://www.pvponline.com/rss/?section=article" rssowl:id="2017199" />
              <outline text="xkcd.com" xmlUrl="http://xkcd.com/rss.xml" rssowl:id="2017200" />
            </outline>
            <outline text="Cinematical" xmlUrl="http://www.cinematical.com/rss.xml" rssowl:id="2017229" />
            <outline text="Gawker" xmlUrl="http://www.gawker.com/index.xml" rssowl:id="2017231" />
            <outline text="People.com Latest News" xmlUrl="http://rss.people.com/web/people/rss/topheadlines/index.xml" rssowl:id="2017233" />
            <outline text="TV Squad" xmlUrl="http://www.tvsquad.com/rss.xml" rssowl:id="2017235" />
            <outline text="Variety.com - Front Page" xmlUrl="http://www.variety.com/rss.asp?categoryid=10" rssowl:id="2017236" />
          </outline>
          <outline text="News" rssowl:isSet="false" rssowl:id="2017238">
            <outline text="HuffingtonPost.com" xmlUrl="http://feeds.huffingtonpost.com/huffingtonpost/raw_feed" rssowl:id="2017239" />
            <outline text="TIME Magazine Online" xmlUrl="http://www.time.com/time/rss/top/0,20326,,00.xml" rssowl:id="2017240" />
            <outline text="Top Stories - Google News" xmlUrl="http://news.google.com/?topic=h&amp;num=3&amp;output=rss" rssowl:id="2017241" />
            <outline text="Yahoo! News" xmlUrl="http://rss.news.yahoo.com/rss/topstories" rssowl:id="2017242" />
            <outline text="Nouveaux Films - SokroStream - Voir Film Streaming 2021 gratuit. Film Complet en Stream VF" xmlUrl="https://sokrostream.tube/streaming-complet-4k/rss.xml" rssowl:id="2201205" />
            <outline text="Zone Streaming" xmlUrl="http://www.zone-streaming.fr/feed" rssowl:id="2201236" />
            <outline text="papystreaming" xmlUrl="https://vvw.papystreaming.stream/feed" rssowl:id="2201238" />
            <outline text="filmstreaming" xmlUrl="https://filmzenstream.cloud/complet-1/rss.xml" rssowl:id="2201390" />
            <outline text="Série streaming et Film streaming en vf ou vostfr complet et HD" xmlUrl="https://french-stream.re/rss.xml" rssowl:id="2201392" />
            <outline text="HDS STREAMING" xmlUrl="https://www.hds-streaming.site/feed" rssowl:id="2201439" />
            <outline text="Site pour télécharger des films gratuitement" xmlUrl="https://9divx.theproxy.ws/b/?https://9divx.theproxy.ws/feed" rssowl:id="2201811" />
            <outline text="Papystreaming" xmlUrl="https://hds.papystreaming.net/feed" rssowl:id="2325815" />
          </outline>
          <outline text="Politics" rssowl:isSet="false" rssowl:id="2017243">
            <outline text="Daily Kos" xmlUrl="http://feeds.dailykos.com/dailykos/index.xml" rssowl:id="2017244" />
            <outline text="GrokLaw" xmlUrl="http://www.groklaw.net/backend/groklaw.rdf" rssowl:id="2017245" />
            <outline text="POLITICO.com: Politics" xmlUrl="http://www.politico.com/rss/politics08.xml" rssowl:id="2017246" />
            <outline text="Talking Points Memo" xmlUrl="http://www.talkingpointsmemo.com/index.xml" rssowl:id="2017247" />
            <outline text="The Agitator" xmlUrl="http://theagitator.com/index.xml" rssowl:id="2017248" />
            <outline text="Think Progress" xmlUrl="http://thinkprogress.org/feed/" rssowl:id="2017249" />
            <outline text="Wonkette" xmlUrl="http://www.wonkette.com/index.xml" rssowl:id="2017250" />
          </outline>
          <outline text="CNN" xmlUrl="http://xml.newsisfree.com/feeds/15/2315.xml" rssowl:id="2017325" />
          <outline text="Reuters News" xmlUrl="http://www.microsite.reuters.com/rss/topnews" rssowl:id="2017327" />
        </outline>
      </body>
    </opml>
""".trimIndent()
