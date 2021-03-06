package telegrambot;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import handlers.DatabaseHandler;
import handlers.TelegramBotHandler;
import kernel.Config;
import kernel.Main;
import objects.Game;
import objects.User;
import wiimmfi.GamesListParser;

public class TelegramBotCommands {
	private final static String[] commands = { "infos", "showplayedgames", "followgame", "unfollowgame" };
	
	public static SendMessage processCommand(User currentUser, String request) {
		final String[] args = request.split(" ");
		final String command = args[0].toLowerCase();

		final SendMessage sendMessage = new SendMessage();
		ReplyKeyboardMarkup replyKeyboardMarkup = null;
		final StringBuilder answer = new StringBuilder();
		if (command.equals("infos")) {
			answer.append("Wiimmfi Notifier v" + Main.version + "\n");
			answer.append("Made with ❤️ by Azlino\n");
			answer.append("\n<b>Stats</b>\n");
			answer.append("Uptime : " + Main.getUptime() + "\n");
			answer.append("Total users count : ");
			answer.append(new CopyOnWriteArrayList<>(TelegramBotHandler.getUsers()).size() + "\n");
			answer.append("Number of games list check this session : ");
			answer.append(Main.checkGamesListCount + "\n");
			answer.append("\n<b>Followed games</b>\n");
			CopyOnWriteArrayList<String> followedGamesUid = new CopyOnWriteArrayList<>(currentUser.getFollowedGamesUid());
			if (followedGamesUid.isEmpty()) {
				answer.append("You don't follow any games");
			} else {
				for (int i = 0; i < followedGamesUid.size(); i++) {
					String gameUid = followedGamesUid.get(i);
					answer.append(gameUid);
					if (i + 1 < followedGamesUid.size()) {
						answer.append(", ");
					}
				}
			}
		} else if (command.equals("showplayedgames")) {
			ArrayList<Game> playedGames = new ArrayList<>();
			for (Game game : new CopyOnWriteArrayList<>(GamesListParser.getGames())) {
				if (game.getOnlineCount() > 0) {
					playedGames.add(game);
				}
			}
			if (playedGames.isEmpty()) {
				answer.append("No games are currently played on Wiimmfi !");
			} else {
				answer.append("<b>Currently played games on Wiimmfi</b>");
				for (Game game : playedGames) {
					answer.append("\n- " + game.getType() + " " + game.getProductionName());
					answer.append(" (" + game.getOnlineCount() + " online)");
				}
			}
		} else if (command.equals("followgame")) {
			ArrayList<Game> notFollowedGames = currentUser.getNotFollowedGames();
			if (notFollowedGames.isEmpty()) {
				answer.append("Error : You are following already all games !");
			} else {
				try {
					String gameUid = args[1].toUpperCase();
					if (args[1].equals("all")) {
						for (Game notFollowedGame : notFollowedGames) {
							if (!currentUser.getFollowedGamesUid().contains(notFollowedGame.getUniqueId())) {
								currentUser.getFollowedGamesUid().add(notFollowedGame.getUniqueId());
								DatabaseHandler.addUserFollowedGame(currentUser.getUserId(), notFollowedGame.getUniqueId());
							}
						}
						answer.append("You are now following the activity of all Wiimmfi games !");
						Main.printNewEvent("User " + currentUser.getUserId() + " follow all games", true);
					} else {
						Game game = GamesListParser.getGameByUniqueId(gameUid);
						if (game == null) {
							throw new Exception();
						}
						if (!currentUser.getFollowedGamesUid().contains(game.getUniqueId())) {
							currentUser.getFollowedGamesUid().add(game.getUniqueId());
							DatabaseHandler.addUserFollowedGame(currentUser.getUserId(), game.getUniqueId());
							answer.append("You are now following the activity of the game : " + game.getProductionName());
							Main.printNewEvent("User " + currentUser.getUserId() + " follow " + game.getUniqueId(), true);
						} else {
							answer.append("Error : You follow already this game !");
						}
					}
				} catch (Exception e) {
					answer.append("Usage : followgame [gameId]\n");
					answer.append("Example for the game Bomberman Blitz : followgame KBBJ\n");
					answer.append("\nIdentifiers of every games can be found there : " + Config.wiimmfiFullGamesListPath + "\n");
					answer.append("\nTip : You can write \"followgame all\" to follow instantly the activity of all Wiimmfi games !");
				}
			}
		} else if (command.equals("unfollowgame")) {
			ArrayList<Game> followedGames = currentUser.getFollowedGames();
			if (followedGames.isEmpty()) {
				answer.append("Error : You are not following any games !");
			} else {
				try {
					String gameUid = args[1].toUpperCase();
					if (args[1].equals("all")) {
						currentUser.getFollowedGamesUid().clear();
						for (Game followedGame : followedGames) {
							DatabaseHandler.deleteUserFollowedGame(currentUser.getUserId(), followedGame.getUniqueId());
						}
						answer.append("You are not following the activity of any games anymore");
						Main.printNewEvent("User " + currentUser.getUserId() + " unfollow all games", true);
					} else {
						Game game = GamesListParser.getGameByUniqueId(gameUid);
						if (game == null) {
							throw new Exception();
						}
						if (currentUser.getFollowedGamesUid().contains(game.getUniqueId())) {
							currentUser.getFollowedGamesUid().remove(game.getUniqueId());
							DatabaseHandler.deleteUserFollowedGame(currentUser.getUserId(), game.getUniqueId());
							answer.append("You are not following anymore the activity of the game : " + game.getProductionName());
							Main.printNewEvent("User " + currentUser.getUserId() + " unfollow " + game.getUniqueId(), true);
						} else {
							answer.append("Error : You were not following this game !");
						}
					}
				} catch (Exception e) {
					answer.append("Usage : unfollowgame [gameId]\n");
					answer.append("Example for the game Bomberman Blitz : unfollowgame KBBJ\n");
					answer.append("\nIdentifiers of every games can be found there : " + Config.wiimmfiFullGamesListPath + "\n");
					answer.append("\nTip : You can write \"unfollowgame all\" to unfollow you from all Wiimmfi games at once !");
				}
			}
		} else {
			answer.append("<b>Available commands</b>");
			for (String availableCommand : getCommands()) {
				answer.append("\n" + availableCommand);
			}
			replyKeyboardMarkup = TelegramBotKeyboards.getCommandsKeyboard();
		}
		if (answer.length() == 0) {
			return null;
		} else {
			sendMessage.setText(answer.toString());
			if (replyKeyboardMarkup != null) {
				sendMessage.setReplyMarkup(replyKeyboardMarkup);
			}
			return sendMessage;
		}
	}

	public static String[] getCommands() {
		return commands;
	}
}
