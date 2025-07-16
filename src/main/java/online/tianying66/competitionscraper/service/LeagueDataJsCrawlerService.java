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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ClassName LeagueDataJsCrawlerService
 * Description TODO
 * @Author zzl
 * @ Version 1.0
 **/
@Slf4j
@Service
public class LeagueDataJsCrawlerService {

    public LeagueData crawlAndParseJs(String url) throws IOException, ScriptException {
        // Fetch the JavaScript file content
        Document doc = Jsoup.connect(url)
                .ignoreContentType(true)
                .timeout(10000)
                .get();
        String jsContent = doc.text();
        log.debug("Fetched content length: {}", jsContent.length());
        // Initialize GraalVM JavaScript engine
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("graal.js");
        if (engine == null) {
            log.error("Graal.js engine not found. Available engines: {}",
                    manager.getEngineFactories().stream()
                            .map(ScriptEngineFactory::getEngineName)
                            .collect(Collectors.joining(", ")));
            throw new IllegalStateException("Graal.js engine is not available");
        }
        log.info("Using script engine: {}", engine.getClass().getName());

        // Ensure jh is initialized
        engine.eval("var jh = jh || {};");

        // Execute the JavaScript
        engine.eval(jsContent);

        // Parse arrLeague
        Object arrLeagueObj = engine.eval("arrLeague");
        log.info("arrLeagueObj type: {}", arrLeagueObj != null ? arrLeagueObj.getClass().getName() : "null");
        LeagueInfo leagueInfo = parseLeagueArray(arrLeagueObj);

        // Parse arrTeam
        Object arrTeamObj = engine.eval("arrTeam");
        log.info("arrTeamObj type: {}", arrTeamObj != null ? arrTeamObj.getClass().getName() : "null");
        List<TeamInfo> teams = parseTeamArray(arrTeamObj);

        // Parse all jh keys (R_1, R_2, etc.)
        List<MatchInfo> matches = new ArrayList<>();
        try {
            Object jhObj = engine.eval("jh");
            String jhObjClassName = jhObj != null ? jhObj.getClass().getName() : "null";
            log.info("jhObj type: {}", jhObjClassName);
            if (jhObj instanceof Map<?, ?> jhMap) {
                for (Object keyObj : jhMap.keySet()) {
                    String key = keyObj.toString();
                    if (key.startsWith("R_")) {
                        log.info("Processing jh key: {}", key);
                        Object matchObj = jhMap.get(key);
                        log.info("matchObj type for {}: {}", key, matchObj != null ? matchObj.getClass().getName() : "null");
                        matches.addAll(parseMatchArray(matchObj));
                    }
                }
            } else {
                log.warn("Unexpected jhObj type: {}", jhObjClassName);
            }
        } catch (ScriptException e) {
            log.warn("Failed to parse jh keys: {}", e.getMessage());
        }

        // Parse totalScore
        Object totalScoreObj = engine.eval("totalScore");
        log.info("totalScoreObj type: {}", totalScoreObj != null ? totalScoreObj.getClass().getName() : "null");
        List<ScoreInfo> totalScores = parseScoreArray(totalScoreObj);

        // Parse homeScore
        Object homeScoreObj = engine.eval("homeScore");
        log.info("homeScoreObj type: {}", homeScoreObj != null ? homeScoreObj.getClass().getName() : "null");
        List<ScoreInfo> homeScores = parseScoreArray(homeScoreObj);

        // Parse scoreColor
        Object scoreColorObj = engine.eval("scoreColor");
        List<ScoreColor> scoreColors = parseScoreColorArray(scoreColorObj);

        // Parse lastUpdateTime
        String lastUpdateTime = (String) engine.eval("lastUpdateTime");

        log.info("Successfully parsed league data");
        return new LeagueData(leagueInfo, teams, matches, totalScores, homeScores, scoreColors, lastUpdateTime);
    }

    private List<ScoreColor> parseScoreColorArray(Object scoreColorObj) {
        List<ScoreColor> colors = new ArrayList<>();
        if (scoreColorObj instanceof List<?> arr) {
            for (Object colorObj : arr) {
                String[] parts = colorObj.toString().split("\\|");
                colors.add(new ScoreColor(parts[0], parts[1], parts[2], parts[3]));
            }
        } else {
            throw new IllegalStateException("Unexpected scoreColorObj type: " + scoreColorObj.getClass().getName());
        }
        return colors;
    }

    private List<ScoreInfo> parseScoreArray(Object scoreObj) {
        List<ScoreInfo> scores = new ArrayList<>();
        if (scoreObj instanceof List<?> arr) {
            for (Object scoreObjItem : arr) {
                List<?> score = (List<?>) scoreObjItem;
                scores.add(new ScoreInfo(
                        Integer.parseInt(score.get(0).toString()),
                        Integer.parseInt(score.get(1).toString()),
                        Integer.parseInt(score.get(2).toString()),
                        Integer.parseInt(score.get(3).toString()),
                        Integer.parseInt(score.get(4).toString()),
                        Integer.parseInt(score.get(5).toString()),
                        Integer.parseInt(score.get(6).toString()),
                        Integer.parseInt(score.get(7).toString()),
                        Integer.parseInt(score.get(8).toString()),
                        score.get(9).toString(),
                        score.get(10).toString(),
                        score.get(11).toString(),
                        Double.parseDouble(score.get(12).toString()),
                        Double.parseDouble(score.get(13).toString()),
                        Double.parseDouble(score.get(14).toString())
//                        score.get(15).toString()
                ));
            }
        } else {
            throw new IllegalStateException("Unexpected scoreObj type: " + (scoreObj != null ? scoreObj.getClass().getName() : "null"));
        }
        return scores;
    }


    private List<MatchInfo> parseMatchArray(Object jhObj) {
        List<MatchInfo> matches = new ArrayList<>();
        if (jhObj instanceof List<?> arr) {
            for (Object matchObj : arr) {
                List<?> match = (List<?>) matchObj;
                matches.add(new MatchInfo(
                        Integer.parseInt(match.get(0).toString()),
                        Integer.parseInt(match.get(1).toString()),
                        Integer.parseInt(match.get(2).toString()),
                        match.get(3).toString(),
                        Integer.parseInt(match.get(4).toString()),
                        Integer.parseInt(match.get(5).toString()),
                        match.get(6).toString(),
                        match.get(7).toString(),
                        match.get(8).toString(),
                        match.get(9).toString(),
                        Integer.parseInt(match.get(14).toString()),
                        Integer.parseInt(match.get(15).toString())
                ));
            }
        } else {
            log.warn("Unexpected jhObjItem type: {}", jhObj != null ? jhObj.getClass().getName() : "null");
        }
        return matches;
    }

    private List<TeamInfo> parseTeamArray(Object arrTeamObj) {
        List<TeamInfo> teams = new ArrayList<>();
        if (arrTeamObj instanceof List<?> arr) {
            for (Object teamObj : arr) {
                List<?> team = (List<?>) teamObj;
                teams.add(new TeamInfo(
                        Integer.parseInt(team.get(0).toString()),
                        team.get(1).toString(),
                        team.get(2).toString(),
                        team.get(3).toString(),
                        team.get(4).toString(),
                        team.get(5).toString(),
                        Integer.parseInt(team.get(6).toString())
                ));
            }
        } else {
            throw new IllegalStateException("Unexpected arrTeamObj type: " + (arrTeamObj != null ? arrTeamObj.getClass().getName() : "null"));
        }
        return teams;
    }


    private LeagueInfo parseLeagueArray(Object arrLeagueObj) {
        if (arrLeagueObj instanceof List<?> arr) {
            return new LeagueInfo(
                    Integer.parseInt(arr.get(0).toString()),
                    arr.get(1).toString(),
                    arr.get(2).toString(),
                    arr.get(3).toString(),
                    arr.get(4).toString(),
                    arr.get(5).toString(),
                    arr.get(6).toString(),
                    Integer.parseInt(arr.get(7).toString()),
                    Integer.parseInt(arr.get(8).toString()),
                    arr.get(9).toString(),
                    arr.get(10).toString(),
                    arr.get(11).toString(),
                    arr.get(12).toString(),
                    Integer.parseInt(arr.get(13).toString())
            );
        } else {
            throw new IllegalStateException("Unexpected arrLeagueObj type: " + (arrLeagueObj != null ? arrLeagueObj.getClass().getName() : "null"));
        }
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LeagueData {
        private LeagueInfo leagueInfo;
        private List<TeamInfo> teams;
        private List<MatchInfo> matches;
        private List<ScoreInfo> totalScores;
        private List<ScoreInfo> homeScores;
        private List<ScoreColor> scoreColors;
        private String lastUpdateTime;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class LeagueInfo {
        private int leagueId;
        private String nameCn;
        private String nameTw;
        private String nameEn;
        private String season;
        private String color;
        private String logo;
        private int totalMatches;
        private int playedMatches;
        private String shortNameCn;
        private String shortNameTw;
        private String shortNameEn;
        private String description;
        private int teamCount;
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class TeamInfo {
        private int teamId;
        private String nameCn;
        private String nameTw;
        private String nameEn;
        private String nickname;
        private String logo;
        private int rank;
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class MatchInfo {
        private int matchId;
        private int leagueId;
        private int round;
        private String matchTime;
        private int homeTeamId;
        private int guestTeamId;
        private String fullScore;
        private String halfScore;
        private String redCards;
        private String yellowCards;
        private int homeGoals;
        private int guestGoals;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ScoreInfo {
        private int rank;
        private int teamId;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int goalDiff;
        private String winRate;
        private String drawRate;
        private String lossRate;
        private double avgGoalsFor;
        private double avgGoalsAgainst;
        private double points;
//        private String status;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class ScoreColor {
        private String color;
        private String labelCn;
        private String labelTw;
        private String labelEn;
    }

}
