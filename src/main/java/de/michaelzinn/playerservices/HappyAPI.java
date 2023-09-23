////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
////
//
//package de.michaelzinn.minecraft.happyapi;
//
//import com.google.common.base.Joiner;
//import com.google.common.base.Optional;
//import com.google.common.collect.Lists;
//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.UnsupportedEncodingException;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Set;
//import java.util.logging.Level;
//import org.bukkit.command.Command;
//import org.bukkit.command.CommandSender;
//import org.bukkit.entity.Player;
//import org.bukkit.plugin.java.JavaPlugin;
//import retrofit.RestAdapter;
//import retrofit.RetrofitError;
//import retrofit.client.Response;
//import retrofit.mime.TypedByteArray;
//import retrofit.mime.TypedInput;
//
//public final class HappyAPI extends JavaPlugin {
//    private static final String SAVE_FILE = "plugins/HappyAPI.save";
//    private Map<String, String> playerUrl = new HashMap();
//    private Map<String, PlayerService> urlService = new HashMap();
//    private Map<Integer, Map<String, Set<String>>> lengthSubstringPlayer = new HashMap();
//
//    public HappyAPI() {
//    }
//
//    void putPlayer(String completePlayerName) {
//        for(int length = 1; length <= completePlayerName.length(); ++length) {
//            String incompletePlayerName = completePlayerName.substring(0, length);
//            this.ensureLength(length);
//            this.ensureSet(incompletePlayerName);
//            ((Set)((Map)this.lengthSubstringPlayer.get(length)).get(incompletePlayerName)).add(completePlayerName);
//        }
//
//    }
//
//    Set<String> getPlayers(String incompletePlayerName) {
//        this.ensureLength(incompletePlayerName.length());
//        this.ensureSet(incompletePlayerName);
//        return (Set)((Map)this.lengthSubstringPlayer.get(incompletePlayerName.length())).get(incompletePlayerName);
//    }
//
//    void removePlayer(String completePlayerName) {
//        for(int length = 1; length <= completePlayerName.length(); ++length) {
//            String incompletePlayerName = completePlayerName.substring(0, length);
//            this.ensureLength(length);
//            Map<String, Set<String>> partialToPlayerName = (Map)this.lengthSubstringPlayer.get(length);
//            this.ensureSet(incompletePlayerName);
//            ((Set)partialToPlayerName.get(incompletePlayerName)).remove(completePlayerName);
//        }
//
//    }
//
//    void ensureLength(int length) {
//        if (!this.lengthSubstringPlayer.containsKey(length)) {
//            this.lengthSubstringPlayer.put(length, new HashMap());
//        }
//
//    }
//
//    void ensureSet(String incompleteName) {
//        if (!((Map)this.lengthSubstringPlayer.get(incompleteName.length())).containsKey(incompleteName)) {
//            ((Map)this.lengthSubstringPlayer.get(incompleteName.length())).put(incompleteName, new HashSet());
//        }
//
//    }
//
//    public void onEnable() {
//        try {
//            this.playerUrl = (Map)this.load();
//            Iterator i$ = this.playerUrl.entrySet().iterator();
//
//            while(i$.hasNext()) {
//                Map.Entry<String, String> entry = (Map.Entry)i$.next();
//                this.registerService((String)entry.getKey(), (String)entry.getValue());
//            }
//        } catch (IOException var3) {
//            this.getLogger().log(Level.WARNING, "Could not load persisted urls: " + var3.getLocalizedMessage());
//        }
//
//    }
//
//    public void onDisable() {
//    }
//
//    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//        String command = cmd.getName();
//        if (sender instanceof Player) {
//            Player player = (Player)sender;
//            String invokingPlayerName = sender.getName();
//            if (command.equalsIgnoreCase("hregister")) {
//                if (args.length == 1) {
//                    if (this.registerService(player, args[0])) {
//                        this.send("Registered successfully! :)", player);
//                        return true;
//                    } else {
//                        this.send("Could not register service.", player);
//                        return false;
//                    }
//                } else {
//                    this.send("Wrong number of arguments!", player);
//                    return false;
//                }
//            } else if (command.equalsIgnoreCase("hunregister")) {
//                if (args.length == 0) {
//                    this.playerUrl.remove(invokingPlayerName);
//                    this.removePlayer(invokingPlayerName);
//                    return true;
//                } else {
//                    this.send("Wrong number of arguments!", player);
//                    return false;
//                }
//            } else if (command.equalsIgnoreCase("h")) {
//                if (args.length >= 1) {
//                    String partialPlayer = args[0];
//                    String apiPlayerName;
//                    if (this.playerUrl.containsKey(partialPlayer)) {
//                        apiPlayerName = partialPlayer;
//                    } else {
//                        Set<String> possibleMatches = this.getPlayers(partialPlayer.toLowerCase());
//                        if (possibleMatches.size() != 1) {
//                            if (possibleMatches.size() == 0) {
//                                this.send("No player service found", player);
//                                return true;
//                            }
//
//                            this.send("Who? " + possibleMatches, player);
//                            return true;
//                        }
//
//                        apiPlayerName = (String)possibleMatches.iterator().next();
//                    }
//
//                    try {
//                        String serviceUrl = (String)this.playerUrl.get(apiPlayerName);
//                        if (!this.urlService.containsKey(serviceUrl)) {
//                            this.registerService(player, serviceUrl);
//                        }
//
//                        Optional<String> response = this.string(((PlayerService)this.urlService.get(serviceUrl)).command((String)null, invokingPlayerName, Joiner.on(" ").join(Lists.newArrayList(args).subList(1, args.length))));
//                        if (response.isPresent()) {
//                            sender.sendMessage((String)response.get());
//                            this.getLogger().log(Level.INFO, "(Response not logged)");
//                            return true;
//                        } else {
//                            this.send("?CONNECTION ERROR: Server send broken data.", player);
//                            return true;
//                        }
//                    } catch (RetrofitError var12) {
//                        this.send("?CONNECTION ERROR: " + var12.getLocalizedMessage(), player);
//                        return true;
//                    }
//                } else {
//                    this.send("Wrong number of arguments!", player);
//                    return false;
//                }
//            } else {
//                this.send("This makes no sense.", player);
//                return false;
//            }
//        } else {
//            this.send("For API provider's convenience, you can only use this as a player at the moment.");
//            return false;
//        }
//    }
//
//    private boolean registerService(String playerName, String something) {
//        return this.registerService(playerName, something, Optional.absent());
//    }
//
//    private boolean registerService(Player player, String something) {
//        return this.registerService(player.getName(), something, Optional.of(player));
//    }
//
//    private boolean registerService(String playerName, String something, Optional<Player> maybePlayer) {
//        RestAdapter restAdapter = (new RestAdapter.Builder()).setEndpoint(something).build();
//        PlayerService service = (PlayerService)restAdapter.create(PlayerService.class);
//
//        try {
//            Optional<String> response = this.string(service.register((String)null, playerName));
//            if (response.isPresent()) {
//                if ("happyAPI accepted :)".equals(response.get())) {
//                    this.playerUrl.put(playerName.toLowerCase(), something);
//                    this.urlService.put(something, service);
//                    this.putPlayer(playerName.toLowerCase());
//
//                    try {
//                        this.save(this.playerUrl);
//                        this.send("service registered", maybePlayer);
//                        return true;
//                    } catch (IOException var8) {
//                        this.send("?Could not save to file: " + var8.getLocalizedMessage(), maybePlayer);
//                        return false;
//                    }
//                } else {
//                    this.send("Wrong server response, server needs to respond true to the /registration call", maybePlayer);
//                    return false;
//                }
//            } else {
//                this.send("Server response is broken");
//                return false;
//            }
//        } catch (RetrofitError var9) {
//            this.send(var9.getLocalizedMessage(), maybePlayer);
//            return false;
//        }
//    }
//
//    private Optional<String> string(Response response) {
//        this.send(response.getBody().toString());
//        TypedInput body = response.getBody();
//        if (body instanceof TypedByteArray) {
//            TypedByteArray bodyByteArray = (TypedByteArray)body;
//
//            try {
//                String registration = new String(bodyByteArray.getBytes(), "UTF8");
//                return Optional.of(registration);
//            } catch (UnsupportedEncodingException var5) {
//                this.send("fail: " + var5.getLocalizedMessage());
//                return Optional.absent();
//            }
//        } else {
//            return Optional.absent();
//        }
//    }
//
//    void send(String text) {
//        this.send(text, Optional.absent());
//    }
//
//    void send(String text, Player player) {
//        this.send(text, Optional.of(player));
//    }
//
//    void send(String text, Optional<Player> maybePlayer) {
//        if (maybePlayer.isPresent()) {
//            ((Player)maybePlayer.get()).sendMessage(text);
//        }
//
//        this.getLogger().log(Level.INFO, text);
//    }
//
//    <T> void save(T obj) throws IOException {
//        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("plugins/HappyAPI.save"));
//        oos.writeObject(obj);
//        oos.flush();
//        oos.close();
//    }
//
//    <T> T load() throws IOException {
//        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("plugins/HappyAPI.save"));
//
//        try {
//            T result = ois.readObject();
//            ois.close();
//            return result;
//        } catch (ClassNotFoundException var3) {
//            throw new IOException(var3);
//        }
//    }
//}
//
