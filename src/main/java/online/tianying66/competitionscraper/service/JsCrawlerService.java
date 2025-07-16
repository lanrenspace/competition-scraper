package online.tianying66.competitionscraper.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @ClassName JsCrawlerService
 * Description TODO
 * @Author zzl
 * @ Version 1.0
 **/
@Slf4j
@Service
public class JsCrawlerService {

    public List<LeagueInfo> crawlAndParseJs(String url) throws IOException, ScriptException {
        // Fetch the JavaScript file content
        Document doc = Jsoup.connect(url).ignoreContentType(true).timeout(5000).get();
        String jsContent = doc.text();

        // Initialize GraalVM JavaScript engine
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("graal.js");
        if (engine == null) {
            log.error("Graal.js engine not found. Available engines: {}",
                    manager.getEngineFactories().stream()
                            .map(ScriptEngineFactory::getEngineName)
                            .collect(java.util.stream.Collectors.joining(", ")));
            throw new IllegalStateException("Graal.js engine is not available");
        }
        log.info("Using script engine: {}", engine.getClass().getName());

        // Execute the JavaScript
        engine.eval(jsContent);

        // Get the array
        Object arrObj = engine.eval("arr");
        log.info("arrObj type: {}", arrObj != null ? arrObj.getClass().getName() : "null");
        if (arrObj == null) {
            throw new IllegalStateException("arrObj is null");
        }

        List<LeagueInfo> leagues = new ArrayList<>();
        // Handle different possible types for arrObj
        if (arrObj instanceof List<?> jsArray) {
            // Handle Java List (possible auto-conversion)
            jsArray.forEach(item -> {
                List<?> dataArray = (List<?>) item;
                String infoId = dataArray.get(0).toString();
                String leagueName = dataArray.get(1).toString();
                String imageUrl = dataArray.get(2).toString();
                String type = dataArray.get(3).toString();
                List<SeasonInfo> seasons = new ArrayList<>();
                if (dataArray.get(4) instanceof List<?> seasonsArray) {
                    for (Object seasonObj : seasonsArray) {
                        String seasonStr = seasonObj.toString();
                        String[] parts = seasonStr.split(",");
                        if (parts.length < 4) {
                            continue;
                        }
                        SeasonInfo season = new SeasonInfo(
                                parts[0].trim(),
                                parts[1].trim(),
                                Integer.parseInt(parts[2].trim()),
                                Integer.parseInt(parts[3].trim()),
                                Arrays.asList(parts).subList(4, parts.length)
                        );
                        seasons.add(season);
                    }
                    leagues.add(new LeagueInfo(infoId, leagueName, imageUrl, type, seasons));
                }
            });
        }
        log.info("Successfully parsed {} leagues", leagues.size());
        return leagues;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LeagueInfo {
        private String infoId;
        private String leagueName;
        private String imageUrl;
        private String type;
        private List<SeasonInfo> seasons;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class SeasonInfo {
        private String leagueId;
        private String leagueName;
        private int type;
        private int status;
        private List<String> seasons;
    }
}
