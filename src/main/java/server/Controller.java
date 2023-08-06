package server;

import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import patch.Constants;
import patch.ItemShinwaManager;
import patch.ItemShinwaManager.ItemShinwa;
import patch.MessageSubCommand;
import patch.interfaces.ISoloer;
import patch.tournament.Tournament;
import real.*;
import threading.Map;
import threading.Message;
import io.Session;
import io.ISessionHandler;
import threading.Server;

import java.io.IOException;
import java.sql.SQLException;

import static patch.ItemShinwaManager.returnXuToSeller;


@SuppressWarnings("ALL")
public class Controller implements ISessionHandler {
    Server server;
    ItemShinwaManager itemShinwaManager;
    LogHistory LogHistory = new LogHistory(this.getClass());

    public Controller() {
        this.server = Server.getInstance();
    }

    @Override
    public void onConnectOK(final Session conn) {
    }

    @Override
    public void onConnectionFail(final Session conn) {
    }

    @Override
    public void onDisconnected(final Session conn) {
        PlayerManager.getInstance().kickSession(conn);
    }

    @SneakyThrows
    @Override
    public void onMessage(final Session conn, final Message message) {
        final ServerController ctl = this.server.controllerManager;
        try {
            final User p = conn.user;

            switch (message.getCommand()) {
                case -30: {
                    if (p != null) {
                        p.messageSubCommand(message);
                        break;
                    }
                    break;
                }
                case -29: {
                    ctl.processGameMessage(conn, message);
                    break;
                }
                case -28: {
                    if (p != null) {
                        p.messageNotMap(message);
                        break;
                    }
                    break;
                }
                case -27: {
                    conn.hansakeMessage();
                    break;
                }
                case -23: {
                    if (p != null) {
                        p.nj.getPlace().Chat(p, message.reader().readUTF());
                        message.cleanup();
                        break;
                    }
                    break;
                }
                case -22: {
                    if (p != null) {
                        p.privateChat(message);
                        break;
                    }
                    break;
                }
                case -21: {
                    if (p != null) {
                        this.server.manager.chatKTG(p, message);
                        break;
                    }
                    break;
                }
                case -20: {
                    if (p != null && p.nj != null) {
                        p.chatParty(message);
                        break;
                    }
                    break;
                }
                case -19: {
                    if (p != null && !p.nj.isDie) {
                        final ClanManager clan = ClanManager.getClanByName(p.nj.clan.clanName);
                        if (clan != null) {
                            clan.chat(p, message);
                        }
                        break;
                    }
                    break;
                }
                case -17: {
                    if (p != null && !p.nj.isDie) {
                        p.nj.getPlace().changeMap(p);
                        message.cleanup();
                        break;
                    }
                    break;
                }
                case -14: {
                    if (p != null && p.nj != null && !p.nj.isDie) {
                        p.nj.getPlace().pickItem(p, message);
                        break;
                    }
                    break;
                }
                case -12: {
                    if (p != null && !p.nj.isDie) {
                        byte index = message.reader().readByte();
                        p.nj.getPlace().leaveItemBackground(p, index);
                        break;
                    }
                    break;
                }
                case -10: {
                    if (p != null && p.nj.isDie && !p.nj.isNhanban) {
                        p.nj.getPlace().wakeUpDieReturn(p);
                        break;
                    }
                    break;
                }
                case -9: {
                    if (p != null && p.nj.isDie && !p.nj.isNhanban) {
                        p.nj.getPlace().DieReturn(p);
                        break;
                    }
                    break;
                }
                case 1: {
                    if (p != null && p.nj != null && !p.nj.isDie) {
                        p.nj.getPlace().moveMessage(p.nj, message.reader().readShort(), message.reader().readShort());
                        break;
                    }
                    break;
                }
                case 4:
                case 73: {
                    attackPlayerVsMob(message, p);
                    break;
                }
                case 11: {
                    if (p != null && !p.nj.isDie) {
                        p.useItem(message);
                        break;
                    }
                    break;
                }
                case 12: {
                    if (p != null && p.nj != null && !p.nj.isDie) {
                        useItem.useItemChangeMap(p, message);
                        break;
                    }
                    break;
                }
                case 13: {
                    if (p != null && !p.nj.isDie) {
                        GameScr.buyItemStore(p, message);
                        break;
                    }
                    break;
                }
                case 14: {
                    if (p != null) {
                        p.SellItemBag(message);
                        break;
                    }
                    break;
                }
                case 15: {
                    if (p != null) {
                        p.itemBodyToBag(message);
                        break;
                    }
                    break;
                }
                case 16: {
                    if (p != null) {
                        p.itemBoxToBag(p, message);
                        break;
                    }
                    break;
                }
                case 17: {
                    if (p != null) {
                        p.itemBagToBox(message);
                        break;
                    }
                    break;
                }
                case 19: {
                    if (p != null) {
                        GameScr.crystalCollect(p, message, true);
                        break;
                    }
                    break;
                }
                case 20: {
                    if (p != null) {
                        GameScr.crystalCollect(p, message, false);
                        break;
                    }
                    break;
                }
                case 21: {
                    if (p != null) {
                        GameScr.UpGrade(p, message);
                        break;
                    }
                    break;
                }
                case 22: {
                    if (p != null) {
                        GameScr.Split(p, message);
                        break;
                    }
                    break;
                }
                case 23: {
                    if (p != null) {
                        p.pleaseInputParty(message);
                        break;
                    }
                    break;
                }
                case 24: {
                    if (p != null) {
                        p.acceptPleaseParty(message);
                        break;
                    }
                    break;
                }
                case 28: {
                    if (p != null && !p.nj.isDie) {
                        p.nj.getPlace().selectUIZone(p, message);
                        break;
                    }
                    break;
                }
                case 29: {
                    if (p != null) {
                        this.server.menu.sendMenu(p, message);
                        break;
                    }
                    break;
                }
                case 36: {
                    if (p != null && !p.nj.isDie) {
                        p.nj.getPlace().openUIZone(p);
                        break;
                    }
                    break;
                }
                case 40: {
                    if (p != null && !p.nj.isDie) {
                        this.server.menu.openUINpc(p, message);
                        break;
                    }
                    break;
                }
                case 41: {
                    if (p != null && p.nj != null && !p.nj.isDie) {

                        useSkill.useSkill(p.nj.get(), message.reader().readShort());
                        break;
                    }
                    break;
                }
                case 42: {
                    if (p != null) {
                        p.requestItemInfo(message);
                        break;
                    }
                    break;
                }
                case 43: {
                    if (p != null && !p.nj.isDie) {
                        p.requestTrade(message);
                        break;
                    }
                    break;
                }
                case 44: {
                    if (p != null && !p.nj.isDie) {
                        p.startTrade(message);
                        break;
                    }
                    break;
                }
                case 45: {
                    if (p != null) {
                        p.lockTrade(message);
                        break;
                    }
                    break;
                }
                case 46: {
                    if (p != null) {
                        p.agreeTrade();
                        break;
                    }
                    break;
                }
                case 47: {
                    if (p != null) {
                        server.menu.selectMenuNpc(p, message);
                        break;
                    }
                    break;
                }
                case 56: {
                    if (p != null) {
                        p.closeTrade();
                        break;
                    }
                    break;
                }
                case 57: {
                    if (p != null) {
                        p.closeLoad();
                        break;
                    }
                    break;
                }
                case 59: {
                    if (p != null) {
                        p.addFriend(message);
                        break;
                    }
                    break;
                }
                case 60: {
                    if (p != null && !p.nj.isDie) {
                        val cloneMessage = message.cloneMessage();
                        p.nj.getPlace().FightMob(p.nj.get(), message);
                        if (p.nj.get().isHuman && p.nj.clone != null && p.nj.clone.isIslive()) {
                            p.nj.getPlace().FightMob(p.nj.clone, (Message) cloneMessage);
                        }
                        break;
                    }
                    break;
                }

                case 61: {
                    if (p != null && !p.nj.isDie) {
                        val cloneMessage = message.cloneMessage();
                        p.nj.getPlace().attackNinja(p.nj.get(), message);
                        if (p.nj.get().isHuman && p.nj.clone != null && p.nj.clone.isIslive()) {
                            p.nj.getPlace().attackNinja(p.nj.clone, (Message) cloneMessage);
                        }
                        break;
                    }
                    break;
                }
                case 65: {
                    if (p != null && !p.nj.isDie) {
                        ISoloer soloer = p.nj.getPlace().getNinja(message.reader().readInt());
                        p.nj.requestSolo(soloer);
                        message.cleanup();
                    }
                    break;
                }
                case 66: {
                    if (p != null && !p.nj.isDie) {
                        p.nj.acceptSolo();
                    }
                    break;
                }
                case 67: {
                    if (p != null && !p.nj.isDie) {
                        p.nj.endSolo();
                    }
                    break;
                }
                case 68: { //cừu sát
                    if (p != null && !p.nj.isDie) {
                        p.addCuuSat(message);
                    }
                    break;
                }

                case 79: {
                    if (p != null) {
                        p.addParty(message);
                        break;
                    }
                    break;
                }
                case 80: {
                    if (p != null) {
                        p.addPartyAccept(message);
                        break;
                    }
                    break;
                }
                case 83: {
                    if (p != null && p.nj != null && p.nj.get().party != null) {
                        p.nj.get().party.exitParty(p.nj);
                        break;
                    }
                    break;
                }
                case 92:
                    handleInputMessage(message, p);
                    break;
                case 93: {
                    if (p != null) {
                        final String playername = message.reader().readUTF();
                        p.viewPlayerMessage(playername);
                        break;
                    }
                    break;
                }
                case 94: {
                    if (p != null) {
                        p.viewOptionPlayers(message);
                        break;
                    }
                    break;
                }
                case 99: {
                    val parnerId = message.reader().readInt();
                    this.goToWaitingBetRoom(p.nj.getPlace().getNinja(parnerId), p.nj);
                    break;
                }
                case 100: {
                    // Xem thi đấu lôi đài
                    int battleId = message.reader().readByte();
                    this.moveToBattleMap(battleId, p);
                    message.cleanup();
                    break;
                }
                case 104: {
                    p.requestItemShinwaInfo(message);
                    break;
                }
                case 108: {
                    if (p != null) {
                        p.itemMonToBag(message);
                        break;
                    }
                    break;
                }
                case 110: {
                    GameScr.LuyenThach(p, message);
                    break;
                }
                case 111: {
                    if (p != null) {
                        GameScr.TinhLuyen(p, message);
                        break;
                    }
                    break;
                }
                case 112: {
                    if (p != null) {
                        GameScr.DichChuyen(p, message);
                        break;
                    }
                    break;
                }
                case 125: {
                    if (p == null) {
                        break;
                    }
                    final byte b = message.reader().readByte();
                    if (b == 1) {
                        GameCanvas.getImgEffect(p.session, message.reader().readShort());
                        break;
                    }
                    if (b == 2) {
                        GameCanvas.getDataEffect(p.session, message.reader().readShort());
                        break;
                    }
                    break;
                }

                case 25: {
                    // Send player info
                    if (p != null) {
                        p.nj.getPlace().sendPlayersInfo(p.nj, message);
                    }
                    break;
                }
                case 102: {
                    // Shinwa
//                     p.session.sendMessageLog("Shinwa Đang Bảo Trì Sửa Chữa !");
                    val indexUI = message.reader().readByte();
                    val price = message.reader().readInt();
                    val item = p.nj.ItemBag[indexUI];
                    if (price < 0) {
                        p.session.sendMessageLog("Không thể thực hiện");
                        return;
                    }
                    int FEE = 5_000;
                    int FEE_GOLD = 1;
                    if (item != null && p.nj.xu >= FEE && p.luong >= FEE_GOLD) {
                        int dem = 0;
                        for (int i = -2; i <= 11; i++) {
                            dem += itemShinwaManager.items.get(i).size();
                        }
                        if (dem > 10000) {
                            p.session.sendMessageLog("Gian hàng Shinwa đã đầy. Vui lòng quay lại sau.");
                            return;
                        }
                        if (item.isExpires) return;
                        ItemShinwa itemShinwa = new ItemShinwa(item, p.nj.name, price);
                        ItemShinwaManager.add(itemShinwa);
                        p.nj.removeItemBag(indexUI);
                        p.nj.upxuMessage(-FEE);
                        p.removeLuong(FEE_GOLD);
                        p.endLoad(true);
                        Service.CharViewInfo(p, false);
                        LogHistory.log1("Shinwa: " + p.nj.name + " đã đăng bán " + item.quantity + " item " + item.id + " với giá: " + price + " xu.");
                    } else {
                        p.sendYellowMessage("Không đủ " + String.format("%,d", FEE) + " xu hoặc " + FEE_GOLD + " lượng để treo ");
                    }
                    break;
                }
                case 105: {
                    // Buy item shinwa
//                    p.session.sendMessageLog("Shinwa Đang Bảo Trì Sửa Chữa !");
                    val itemId = message.reader().readInt();
                    val itemShinwa = ItemShinwaManager.findItemById(itemId);

                    if (!itemShinwa.isExpired() && itemShinwa.getPrice() <= p.nj.getXu()) {
                        final byte bagNull = p.nj.getAvailableBag();
                        if (bagNull == 0) {
                            p.session.sendMessageLog("Hành trang không đủ chổ trống");
                            return;
                        }
                        p.endLoad(true);
                        if (p.nj.xu < itemShinwa.getPrice()) {
                            // Khong du xu
                            p.sendYellowMessage("Không đủ xu để mua item");
                            return;
                        }

                        p.nj.upxuMessage(-itemShinwa.getPrice());
                        p.nj.addItemBag(false, itemShinwa.getItem());
                        returnXuToSeller(itemShinwa);
                        LogHistory.log1("Shinwa: " + p.nj.name + " đã mua " + itemShinwa.getItem().quantity + " item " + itemShinwa.getItem().id + " với giá: " + itemShinwa.getPrice() + " xu.");
                        final Message m = new Message(57);
                        m.writer().flush();
                        p.session.sendMessage(m);
                        m.cleanup();
                    }
                    break;
                }
                case 106: {
                    p.acceptInviteGT(message.reader().readInt());
                    break;
                }
                case 107: {
                    yesNoDlg(p, message);
                    break;
                }
                case 124: {
                    // Luyen ngoc
                    GameScr.ngocFeature(p, message);
                    break;
                }

                case 121: {
                    val index = message.reader().readByte();
                    val ninjaName = message.reader().readUTF();

                    if (index == 0) {
                        GameScr.requestRankedInfo(p, ninjaName);
                    } else {
//                         Thach dau
                        if (p.nj.getTournamentData().isCanGoNext()) {
                            if (p.nj.name.equals(ninjaName)) {
                                if (p.nj.getTournamentData().getRanked() == 1) {
                                    p.sendYellowMessage("Bạn là nhất rồi !!");
                                } else {
                                    p.sendYellowMessage("Bạn không thể chiến đấu với mình");
                                }
                                break;
                            }

                            final Tournament tournament = Tournament.getTypeTournament(p.nj.getLevel());
                            if (!tournament.checkBusy(ninjaName)) {
                                tournament.enter(p.nj, ninjaName);
                            } else {
                                p.sendYellowMessage("Đối thủ đang thi đấu với một người khác vui lòng đợi trong giây lát");
                            }

                        } else {
                            p.nj.getPlace().chatNPC(p, 4, "Thất bại là mẹ thành công ta biết con hơi buồn nhưng một sự thật đáng buồn là con hãy quay lại vào ngày hôm sau");
                        }
                    }

                    break;
                }
                case 113: {
                    //
                    if (p != null && p.nj != null && p.nj.candyBattle != null) {
                        val id = message.reader().readShort();
                        System.out.println(id);
                        p.nj.candyBattle.catKeo(p.nj, id);
                    }
                    break;
                }

                default: {
                    util.Debug("NOT MATCH " + message.getCommand());
                }
            }
            message.cleanup();
        } catch (Exception ex) {
            System.out.println("ERROR Process message");
            ex.printStackTrace();
        }
    }


    private void attackPlayerVsMob(Message message, User p) throws IOException {
        if (p != null && !p.nj.isDie) {
            val _ninja = p.nj;

            if (p != null && _ninja != null && _ninja.getCSkill() != -1) {
                if (_ninja.ItemBody[1] == null) {
                    p.sendYellowMessage("Vũ khí không hợp lệ");
                    return;
                }

                val template = _ninja.getCSkillTemplate();

                if (_ninja.mp < _ninja.getCSkillTemplate().manaUse) {
                    p.nj.getPlace().updateMp(p);
                    MessageSubCommand.sendMP(p.nj);
                    return;
                }
                if (_ninja.getSkills().size() > 0) {
                    byte size = message.reader().readByte();

                    if (size >= 0 && size <= template.maxFight) {
                        Mob[] arrMob = new Mob[size];
                        Ninja[] arrNinja = new Ninja[_ninja.getCSkillTemplate().maxFight];
                        try {
                            byte i;
                            for (i = 0; i < arrMob.length &&
                                    i < template.maxFight; i = (byte) (i + 1)) {

                                arrMob[i] = _ninja.getPlace().getMob(message.reader().readUnsignedByte());
                                if (arrMob[i] == null) continue;
                            }
                            for (i = 0; i < arrNinja.length &&
                                    i < template.maxFight; i = (byte) (i + 1)) {
                                arrNinja[i] = _ninja.getPlace().getNinja(message.reader().readInt());
                            }
                        } catch (Exception exception) {
                        }
                        _ninja.getPlace().PlayerAttack(_ninja, arrMob, arrNinja);
                    }
                }
            }
            return;
        }
    }

    private void moveToBattleMap(int battleId, User p) {
        if (Battle.battles.containsKey(battleId)) {

            val battle = Battle.battles.get(battleId);
            p.nj.isBattleViewer = true;
            battle.addViewerIfNotInMatch(p.nj);
            p.nj.enterSamePlace(battle.getPlace(), null);

        }
    }

    private void handleInputMessage(Message message, User p) throws CloneNotSupportedException, IOException {
        // Input
        var cloneMessage = message.cloneMessage();
        var menuId = message.reader().readShort();
        val str = message.reader().readUTF();

        if (menuId == 1) {
            // Teleport
            teleport(p, str);
        } else if (menuId == 2) {
            var inputNinja = p.nj.getPlace().getNinja(str);
            if (inputNinja != null) {
                if (p.nj.party != null && p.nj.party.getNinja(inputNinja.id) != null) {
                    // Cung team
                    p.nj.getPlace().chatNPC(p, (short) 0, "Đối thủ cùng team với nhà người tính troll ta à");
                    return;
                }
                // Request battle
                p.nj.getPlace().chatNPC(p, (short) 0, "Ta đã gửi lời khiêu chiến của con đến đối thủ rồi");
                sendRequestBattleToAnother(inputNinja, p.nj, -157);
            } else {
                p.nj.getPlace().chatNPC(p, (short) 0, "Ta không tìm thấy đối thủ của con trong khu này");
            }
        } else if (menuId == 3) {
            // Đặt cược lôi đài
            if (p.nj.party != null) {
                if (p.nj.party.master != p.nj.id) {
                    p.sendYellowMessage("Chỉ có trưởng nhóm mới có thể đặt cược được");
                }
            }
            if (p.nj.hasBattle()) {
                p.nj.getBattle().setXu(Long.parseLong(str), p.nj.party != null ? p.nj.party : p.nj);
            } else {
                p.nj.getClanBattle().setXu(p.nj, Integer.parseInt(str.replace(",", "").replace(".", "").trim()));
            }

        } else if (menuId == 4) {
            final ClanManager clan = ClanManager.getClanByName(str);
            if (clan != null) {
                val tocTruong = clan.members.stream().filter(m -> {
                    val ninja = m.getNinja();
                    if (ninja == null) return false;
                    if (ninja.clan.typeclan == Constants.TOC_TRUONG)
                        return true;
                    return false;
                }).map(c -> c.getNinja()).findFirst().orElse(null);
                if (tocTruong != null) {
                    final Ninja n = p.nj.getPlace().getNinja(tocTruong.name);
                    if (n != null) {
                        p.nj.getPlace().chatNPC(p, (short) 32, "Ta đã gửi lời mời thách đấu của ngươi đến tộc trưởng gia tộc " + str);
                        sendRequestBattleToAnother(tocTruong, p.nj, -150);
                    } else {
                        p.nj.getPlace().chatNPC(p, (short) 32, "Tộc trưởng không cùng khu với bạn");
                    }
                } else {
                    p.sendYellowMessage("Tộc trưởng gia tộc bạn thách đấu không online");
                }
            } else {
                p.sendYellowMessage("Không tìm thấy gia tộc trên hệ thống");
            }
        } else if (menuId == 5) {
            try {
                SQLManager.executeUpdate("UPDATE player SET `status` = 'lock' WHERE `username`='" + p.username + "' LIMIT 1");
                p.session.disconnect();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } else if (p != null) {
            Draw.Draw(p, (Message) cloneMessage);
            return;
        }
        return;
    }

    private void teleport(User p, String str) throws IOException {
        Ninja ninja = PlayerManager.getInstance().getNinja(str);
        if (ninja == null) {
            p.sendYellowMessage("Người chơi đã offline");
            return;
        }
        final Map map = Server.getMapById(ninja.getMapid());
        if (map != null && (map.isLangCo() || Map.isCaveMap(map.id) || map.VDMQ() || map.loiDaiMap())) {
            p.sendYellowMessage("Con không thể di chuyển đến khu vực cấm này nếu đi vào sẽ phải có bình oxy mới được");
            return;
        }

        p.nj.getPlace().leave(p);
        ninja.getPlace().Enter(p);
        p.nj.x = ninja.x;
        p.nj.y = ninja.y;
        ninja.getPlace().sendXYPlayer(p);
    }

    private void sendRequestBattleToAnother(Ninja friendNinja, Ninja p, int id) throws IOException {

        val m = new Message(id);
        m.writer().writeInt(p.id);
        friendNinja.p.sendMessage(m);
        m.cleanup();
    }

    private void goToWaitingBetRoom(Ninja partner, Ninja me) throws IOException {
        Battle battle = new Battle(partner, me);
        battle.tick();
        battle.enter();
    }

    public static void yesNoDlg(User player, Message m) {
        try {
            if(player != null && player.nj != null && player.session != null && !player.nj.isDie && m != null && m.reader().available() > 0) {
                int type = m.reader().readByte();
                switch (type) {
                    case 0: {
                        GameScr.NangMat(player, player.nj.ItemBody[14], 0);
                        break;
                    }
                    case 1: {
                        GameScr.NangMat(player, player.nj.ItemBody[14], 1);
                        break;
                    }
                    case 2: {
                        GameScr.HuyNhiemVuDanhVong(player);
                        break;
                    }
                    case 3: {
                        Item im = player.nj.ItemMuaLai.get(player.indexMenuBox);
                        int xu = 10000;
                        if (im.isTypeBody()) {
                            xu = im.id * 100;
                        }
                        if (player.nj.xu < xu) {
                            player.session.sendMessageLog("Ngươi không đủ " + xu + " xu để mua lại vật phẩm này!");
                            return;
                        }
                        player.nj.addItemBag(false, im);
                        player.nj.upXuMessage(-xu);
                        player.nj.ItemMuaLai.remove(player.indexMenuBox);
                        player.endLoad(true);
                        break;
                    }
                    case 4:{
                        
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(m != null) {
                m.cleanup();
            }
        }
    }
}
