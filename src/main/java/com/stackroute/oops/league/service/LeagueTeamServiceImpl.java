package com.stackroute.oops.league.service;

import com.stackroute.oops.league.dao.PlayerDao;
import com.stackroute.oops.league.dao.PlayerDaoImpl;
import com.stackroute.oops.league.dao.PlayerTeamDao;
import com.stackroute.oops.league.dao.PlayerTeamDaoImpl;
import com.stackroute.oops.league.exception.PlayerAlreadyAllottedException;
import com.stackroute.oops.league.exception.PlayerAlreadyExistsException;
import com.stackroute.oops.league.exception.PlayerNotFoundException;
import com.stackroute.oops.league.exception.TeamAlreadyFormedException;
import com.stackroute.oops.league.model.Player;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class implements leagueTeamService
 * This has four fields: playerDao, playerTeamDao and registeredPlayerList and playerTeamSet
 */
public class LeagueTeamServiceImpl implements LeagueTeamService {
    private PlayerDao playerDao;
    private PlayerTeamDao playerTeamDao;
    private List<Player> registeredPlayerList;

    /**
     * static nested class to initialize admin credentials
     * admin name='admin' and password='pass'
     */
    static class AdminCredentials {
        private static String admin = "admin";
        private static String password = "pass";
    }
    /**
     * Constructor to initialize playerDao, playerTeamDao
     * empty ArrayList for registeredPlayerList and empty TreeSet for playerTeamSet
     */
    public LeagueTeamServiceImpl() {
        playerDao=new PlayerDaoImpl();
        playerTeamDao=new PlayerTeamDaoImpl();
        registeredPlayerList=new ArrayList<>();

    }

    //Add player data to file using PlayerDao object
    @Override
    public boolean addPlayer(Player player) throws PlayerAlreadyExistsException, IOException {
        playerDao.addPlayer(player);
        return true;
    }

    /**
     * Register the player for the given teamTitle
     * Throws PlayerNotFoundException if the player does not exists
     * Throws PlayerAlreadyAllottedException if the player is already allotted to team
     * Throws TeamAlreadyFormedException if the maximum number of players has reached for the given teamTitle
     * Returns null if there no players available in the file "player.csv"
     * Returns "Registered" for successful registration
     * Returns "Invalid credentials" when player credentials are wrong
     */
    @Override
    public synchronized String registerPlayerToLeague(String playerId, String password, LeagueTeamTitles teamTitle)
            throws PlayerNotFoundException, TeamAlreadyFormedException, PlayerAlreadyAllottedException, IOException {
        if(playerDao.getAllPlayers().size()==0){
            return null;
        }
        Player player=playerDao.findPlayer(playerId);
        if(player==null){
            throw new PlayerNotFoundException();
        }
        if(!player.getPassword().equals(password)){
            return "Invalid credentials";
        }
        if(playerTeamDao.getAllPlayerTeams().stream().anyMatch(play->play.getPlayerId().equals(playerId)))
               throw new PlayerAlreadyAllottedException();
        if(getNumberOfPlayersInTeam(teamTitle)>=11){
            throw new TeamAlreadyFormedException();
        }

        player.setTeamTitle(teamTitle.getValue());
        registeredPlayerList.add(player);
        return "Registered";
    }

    /**
     * Return the list of all registered players
     */
    @Override
    public List<Player> getAllRegisteredPlayers() {
        return registeredPlayerList;
    }


    /**
     * Return the existing number of players for the given title
     */
    @Override
    public int getExistingNumberOfPlayersInTeam(LeagueTeamTitles teamTitle) throws IOException {
        Predicate<Player> predicate=(player)->player.getTeamTitle()!=null && player.getTeamTitle().equals(teamTitle.getValue());
         return playerDao.getAllPlayers()
                .stream()
                .filter(predicate)
                .collect(Collectors.toList()).size();

    }



    /**
     * Admin credentials are authenticated and registered players are allotted to requested teams if available
     * If the requested teams are already formed,admin randomly allocates to other available teams
     * PlayerTeam object is added to "finalteam.csv" file allotted by the admin using PlayerTeamDao
     * Return "No player is registered" when registeredPlayerList is empty
     * Throw TeamAlreadyFormedException when maximum number is reached for all teams
     * Return "Players allotted to teams" when registered players are successfully allotted
     * Return "Invalid credentials for admin" when admin credentials are wrong
     */
    @Override
    public String allotPlayersToTeam(String adminName, String password, LeagueTeamTitles teamTitle) throws TeamAlreadyFormedException, PlayerNotFoundException, IOException {
        if((adminName.equals(AdminCredentials.admin))&& password.equals(AdminCredentials.password)){
            if(registeredPlayerList.isEmpty()){
                return "No player is registered";
            }
            if(getExistingNumberOfPlayersInTeam(teamTitle)>=11){
                throw new TeamAlreadyFormedException();
            }
            else{
               addPlayerToTeamTitle(0,getAllRegisteredPlayers(),teamTitle);
            }

            return "Players allotted to teams";
        }
        return "Invalid credentials for admin";
    }



    private boolean checkPlayerTeam(Player play2) throws IOException {
           Optional<Player> player= playerDao.getAllPlayers().stream().filter(play->play.getPlayerId().equals(play2.getPlayerId())).findAny();
           if(player.get().getTeamTitle()==null)
            return true;
        return false;
    }

    private void addPlayerToTeamTitle( int regPlayerIndex,List<Player> regPlayer,LeagueTeamTitles teamTitle) throws IOException, PlayerNotFoundException, TeamAlreadyFormedException {
          while (regPlayerIndex!=regPlayer.size()){
              if(getExistingNumberOfPlayersInTeam(teamTitle)<11){
                  if(checkPlayerTeam(regPlayer.get(regPlayerIndex))){
                      addPlayerToTeamTitle(regPlayer.get(regPlayerIndex),teamTitle);
                  }
                  regPlayerIndex++;
              }
              else{
                  throw new TeamAlreadyFormedException();
              }
        }
    }

    private void addPlayerToTeamTitle(Player regPlayer,LeagueTeamTitles teamTitle) throws IOException, PlayerNotFoundException {
        regPlayer.setTeamTitle(teamTitle.getValue());
        playerTeamDao.addPlayerToTeam(regPlayer);
    }

    private int getNumberOfPlayersInTeam(LeagueTeamTitles teamTitles) throws IOException {
        return playerTeamDao.getAllPlayerTeams().stream().filter(play->play.getTeamTitle().equals(teamTitles.getValue())).collect(Collectors.toList()).size();
    }

}

