package real;

import io.Session;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import patch.Constants;
import patch.Friend;
import patch.ItemShinwaManager;
import patch.battle.ClanBattle;
import patch.clan.ClanTerritoryData;
import patch.interfaces.SendMessage;
import patch.tournament.Tournament;
import server.*;
import tasks.TaskHandle;
import threading.Manager;
import threading.Map;
import threading.Message;
import threading.Server;
import real.MobData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static real.User.TypeTBLOption.ALL_MAP;
import static server.util.*;

public class User extends Actor implements SendMessage {
    public static final String TẤT_CẢ_CÁC_KHU_ĐẶT_CƯỢC_ĐỀU_FULL = "Tất cả các khu đặt cược đều full";
    public static long MIN_TIME_RESET_POINT;
    public String username;
    public String version;
    public Session session;
    public Ninja nj;
    public String passold;
    public String passnew;
    public long expiredTime;
    public int indexMenuBox;

    private ClanTerritoryData clanTerritoryData;

    private long lastTimeUseItem = System.currentTimeMillis();
    Server server;

    public boolean nhanQua = false;
    private int clanTerritoryId;
    LogHistory LogHistory = new LogHistory(this.getClass());
    public int coin;
    public int ddhn = 0;

    public enum TypeTBLOption {
        NOT_USE(-1),
        $240(240),
        $480(480),
        ALL_MAP(888888),
        USEFUL(2),
        PICK_ALL(3);
        private int value;

        TypeTBLOption(int i) {
            this.value = i;
        }

        public int getValue() {
            return value;
        }
    }

    public TypeTBLOption typeTBLOption = ALL_MAP;
    public boolean activeTBL = false;
    public boolean filter = false;

    public User() {
        this.username = null;
        this.version = null;
        this.session = null;
        this.setNj(null);
        this.passold = "";
        this.passnew = "";
        this.setClanTerritoryId(-1);
        this.server = Server.getInstance();

    }

    public boolean containsItem(int id) {
        for (Item item : this.nj.ItemBag) {
            if (item != null && item.id == id) {
                return true;
            }
        }
        return false;
    }


    public void cleanup() {
        this.session = null;
        this.setClanTerritoryData(null);
    }

    public synchronized int upluong(long x) {
        if (x < 0) {
            this.nj.diemdungluong += (double)Math.abs(x);
//            this.sendYellowMessage("Bạn nhận được " + (double)Math.abs(x) + " điểm dùng lượng");
        } else if (x > 0) {
            LogHistory.log4("Upluong: " + this.nj.name + " tang " + x + " luong");
        }
        final long luongnew = this.luong + x;
        if (luongnew > 2000000000L) {
            x = 2000000000 - this.luong;
        } else if (luongnew < -2000000000L) {
            x = -2000000000 - this.luong;
        }
        this.luong += (int) x;
        if (this.luong < 0) {
            try {
                SQLManager.executeUpdate("UPDATE player SET `status` = 'lock' WHERE `username`='" + username + "' LIMIT 1");
                this.session.disconnect();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        return (int) x;
    }
    // coin //
    public synchronized int upcoin(long x) {
        final long coinnew = this.coin + x;
        if (coinnew > 2000000000L) {
            x = 2000000000 - this.coin;
        } else if (coinnew < -2000000000L) {
            x = -2000000000 - this.coin;
        }
        this.coin += (int) x;
        return (int) x;
    }
    //  End //


    public static User login(final Session conn, final String user, final String pass) {
        final User[] u = new User[]{null};
        val query = "SELECT * FROM `player` WHERE (`username`LIKE'" + user + "' AND `password`LIKE'" + pass + "');";
        SQLManager.executeQuery(query, (red) -> {
            if (red != null && red.first()) {
                final int iddb = red.getInt("id");
                final String username = red.getString("username");
                final int luong = red.getInt("luong");
                final byte lock = red.getByte("lock");
                final String status = red.getString("status");
                final int coin = red.getInt("coin");
                final int ddhn = red.getInt("ddhn");
                if (status.equals("wait")) {
                    conn.sendMessageLog("Tài khoản của bạn chưa kích hoạt. Để biết thêm thông tin hãy liên hệ ADMIN");
                    u[0] = null;
                    return;
                }
                
                if (status.equals("lock")) {
                    conn.sendMessageLog("Tài khoản của bạn đã bị khoá do bug. Để biết thêm thông tin hãy liên hệ ADMIN");
                    u[0] = null;
                    return;
                }
                if (status.equals("spam")) {
                    conn.sendMessageLog("Tài khoản của bạn đã bị khoá do spam. Liên hệ ADMIN Để Được Ban Ip Vĩnh Viễn  !! .");
                    u[0] = null;
                    return;
                }

                if (lock == 1) {
                    conn.sendMessageLog("Tài khoản của bạn đã bị khóa . Để biết thêm thông tin hãy liên hệ ADMIN");
                    u[0] = null;
                    return;
                }
                if (!util.CheckString(user, "^[a-zA-Z0-9]+$") || !util.CheckString(pass, "^[a-zA-Z0-9]+$")) {
                    conn.sendMessageLog("Thông tin tài khoản hoặc mật khẩu không chính xác !");
                    u[0] = null;
                    return;
                }
                if (PlayerManager.timeWaitLogin.containsKey(username)) {
                if (System.currentTimeMillis() < (Long)PlayerManager.timeWaitLogin.get(username)) {
                conn.sendMessageLog("Bạn chỉ có thể đăng nhập lại vào tài khoản sau " + ((Long)PlayerManager.timeWaitLogin.get(username) - System.currentTimeMillis()) / 1000L + "s nữa");
                u[0] = null;
                return;
                }
                PlayerManager.timeWaitLogin.remove(username);
                }
                final JSONArray jarr = (JSONArray) JSONValue.parse(red.getString("ninja"));
                User p = PlayerManager.getInstance().getPlayer(user);
                if (p != null) {
                    p.session.sendMessageLog("Có người đăng nhập vào tài khoản của bạn");
                    PlayerManager.getInstance().kickSession(p.session);
                    if (p.session != null) {
                        p.session.disconnect();
                    }
                    u[0] = null;
                    conn.sendMessageLog("Bạn đang đăng nhập tại máy khác. Hãy thử đăng nhập lại");
                    return;
                }
                p = new User();
                p.session = conn;
                p.id = iddb;
                p.username = username;
                p.luong = luong;
                p.passold = pass;
                p.coin = coin;
                p.ddhn = ddhn;
                try {
                    p.setClanTerritoryId(red.getInt("clanTerritoryId"));
                } catch (Exception e) {
                    p.setClanTerritoryId(-1);
                    e.printStackTrace();
                }

                for (byte i = 0; i < jarr.size(); ++i) {
                    p.sortNinja[i] = jarr.get(i).toString();
                }
                PlayerManager.getInstance().put(p);
                u[0] = p;
            } else {
                conn.sendMessageLog("Thông tin tài khoản hoặc mật khẩu không chính xác");
            }
        });
        return u[0];
    }


    public void messageSubCommand(final Message m) throws IOException {
        final byte b = m.reader().readByte();
//        Debug("Cmd -30->" + b);
        switch (b) {
            case -109: {
                this.pluspPoint(m);
                break;
            }
            case -108: {
                this.plusSkillpoints(m);
                break;
            }
            case -107: {
                this.nj.sortBag();
                break;
            }
            case -106: {
                this.nj.sortBox();
                break;
            }
            case -105: {
                this.xuBagtoBox(m);
                break;
            }
            case -104: {
                this.xuBoxtoBag(m);
                break;
            }
            case -103: {
                GameScr.ItemInfo(this, m);
                break;
            }
            case -93: {
                this.nj.getPlace().changerTypePK(this, m);
                break;
            }
            case -88: {
                if (this.nj != null) {
                    this.createParty();
                    break;
                }
                break;
            }
            case -87: {
                if (this.nj != null) {
                    this.changeTeamLeaderParty(m);
                    break;
                }
                break;
            }
            case -86: {
                if (this.nj != null) {
                    this.moveMemberParty(m);
                    break;
                }
                break;
            }
            case -85: {
                this.viewFriend();
                break;
            }
            case -83: {
                this.deleteFriend(m);
                break;
            }
            case -79: {
                useSkill.buffLive(this, m);
                break;
            }
            case -77: {
                if (this.nj != null && this.nj.getPlace() != null) {
                    this.nj.getPlace().openFindParty(this);
                    break;
                }
                break;
            }
            case -67: {
                this.pasteSkill(m);
                break;
            }
            case -65: {
                GameScr.sendSkill(this, m.reader().readUTF());
                break;
            }
            case -63: {
                this.clanInvite(m);
                break;
            }
            case -62: {
                this.acceptInviteClan(m);
                break;
            }
            case -61: {
                this.clanPlease(m);
                break;
            }
            case -60: {
                this.acceptPleaseClan(m);
                break;
            }
            default:
                util.Debug("NOT MATCH sub command b");
        }
    }

    @SneakyThrows
    public void sendInfo() {
        sendInfo(true);
    }

    public void sendInfo(boolean enter) throws IOException {
//        if (nj.isHuman)
//            restPoint();
        this.nj.hp = this.nj.getMaxHP();
        this.nj.mp = this.nj.getMaxMP();
        final Message m = new Message(-30);
        m.writer().writeByte(-127);
        m.writer().writeInt(this.nj.id);
        m.writer().writeUTF(this.nj.clan.clanName);
        if (!this.nj.clan.clanName.isEmpty()) {
            m.writer().writeByte(this.nj.clan.typeclan);
        }
        m.writer().writeByte(this.nj.getTaskId());
        m.writer().writeByte(this.nj.gender);
        m.writer().writeShort(this.nj.head);
        m.writer().writeByte(this.nj.speed());
        m.writer().writeUTF(this.nj.name);
        m.writer().writeByte(this.nj.pk);
        m.writer().writeByte(this.nj.getTypepk());
        m.writer().writeInt(this.nj.getMaxHP());
        m.writer().writeInt(this.nj.hp);
        m.writer().writeInt(this.nj.getMaxMP());
        m.writer().writeInt(this.nj.mp);
        m.writer().writeLong(this.nj.getExp());
        m.writer().writeLong(this.nj.expdown);

        m.writer().writeShort(this.nj.eff5buffHP());
        m.writer().writeShort(this.nj.eff5buffMP());
        m.writer().writeByte(this.nj.nclass);
        m.writer().writeShort(this.nj.get().getPpoint());
        m.writer().writeShort(this.nj.get().getPotential0());
        m.writer().writeShort(this.nj.get().getPotential1());
        m.writer().writeInt(this.nj.get().getPotential2());
        m.writer().writeInt(this.nj.get().getPotential3());
        m.writer().writeShort(this.nj.get().getSpoint());
        m.writer().writeByte(this.nj.getSkills().size());
        for (short i = 0; i < this.nj.getSkills().size(); ++i) {
            final Skill skill = this.nj.getSkills().get(i);
            m.writer().writeShort(SkillData.Templates(skill.id, skill.point).skillId);
        }
        m.writer().writeInt(this.nj.xu);
        m.writer().writeInt(this.nj.yen);
        m.writer().writeInt(this.luong);
        m.writer().writeByte(this.nj.maxluggage);
        for (int j = 0; j < this.nj.maxluggage; ++j) {
            final Item item = this.nj.ItemBag[j];
            if (item != null) {
                m.writer().writeShort(item.id);
                m.writer().writeBoolean(item.isLock());
                if (ItemData.isTypeBody(item.id) || ItemData.isTypeMounts(item.id) || ItemData.isTypeNgocKham(item.id)) {
                    m.writer().writeByte(item.getUpgrade());
                }
                m.writer().writeBoolean(item.isExpires);
                m.writer().writeShort(item.quantity);
            } else {
                m.writer().writeShort(-1);
            }
        }
        for (int k = 0; k < 16; ++k) {
            final Item item = this.nj.ItemBody[k];
            if (item != null) {
                m.writer().writeShort(item.id);
                m.writer().writeByte(item.getUpgrade());
                m.writer().writeByte(item.sys);
            } else {
                m.writer().writeShort(-1);
            }
        }
        m.writer().writeBoolean(this.nj.isHuman);
        m.writer().writeBoolean(this.nj.isNhanban);
        m.writer().writeShort(-1);
        m.writer().writeShort(-1);
        m.writer().writeShort(-1);
        m.writer().writeShort(-1);
//        Item item0 = this.nj.get().ItemBody[18];//Đầu thân chân
//        if (item0 != null) {
//            if (item0.id == 795) {//Thiên Nguyệt Chi Nữ
//                m.writer().writeShort(37);
//                m.writer().writeShort(38);
//                m.writer().writeShort(39);
//            } else if (item0.id == 796) {//Nhật Tử Lam Phong
//                m.writer().writeShort(40);
//                m.writer().writeShort(41);
//                m.writer().writeShort(42);
//            } else if (item0.id == 804) {//Hajiro
//                m.writer().writeShort(58);
//                m.writer().writeShort(59);
//                m.writer().writeShort(60);
//            } else if (item0.id == 805) {//Shiraiji
//                m.writer().writeShort(55);
//                m.writer().writeShort(56);
//                m.writer().writeShort(57);
//            } else if (item0.id == 830) {//Mặt nạ hổ
//                m.writer().writeShort(69-this.nj.gender*3);
//                m.writer().writeShort(70-this.nj.gender*3);
//                m.writer().writeShort(71-this.nj.gender*3);
//            } else {
//                m.writer().writeShort(-1);
//                m.writer().writeShort(-1);
//                m.writer().writeShort(-1);
//            }
//        } else {
//            m.writer().writeShort(-1);
//            m.writer().writeShort(-1);
//            m.writer().writeShort(-1);
//        }
//        Item item3 = this.nj.get().ItemBody[17];//Vũ khí
//        if (item3 != null) {
//            if (item3.id == 799) {//Gậy Mặt Trăng
//                m.writer().writeShort(44);
//            } else if (item3.id == 800) {//Gậy Trái tim
//                m.writer().writeShort(46);
//            } else {
//                m.writer().writeShort(-1);
//            }
//        } else {
//            m.writer().writeShort(-1);
//        }
//        Item item4 = this.nj.get().ItemBody[12];//Yoroi
//        if (item4 != null) {
//            if (item4.id == 797) {//Hakairo Yoroi
//                m.writer().writeShort(43);
//            } else {
//                m.writer().writeShort(-1);
//            }
//        } else {
//            m.writer().writeShort(-1);
//        }
//        m.writer().writeShort(-1);//name
//        Item item6 = this.nj.get().ItemMounts[4];//Thú cưỡi
//        if (item6 != null) {
//            if (item6.id == 798) {//Lân Sư Vũ
//                m.writer().writeShort(36);
//            } else if (item6.id == 801) {//Xích Tử Mã
//                m.writer().writeShort(47);
//            } else if (item6.id == 802) {//Tà Linh Mã
//                m.writer().writeShort(48);
//            } else if (item6.id == 803) {//Phong Thương Mã
//                m.writer().writeShort(49);
//            } else if (item6.id == 827) {//Phượng Hoàng Băng
//                m.writer().writeShort(63);
//            } else if (item6.id == 831) {
//                m.writer().writeShort(72);
//            } else {
//                m.writer().writeShort(-1);
//            }
//        } else {
//            m.writer().writeShort(-1);
//        }
//        m.writer().writeShort(-1);//rank
//        Item item8 = this.nj.get().ItemBody[27];//mặt nạ
//        if (item8 != null) {
//            if (item8.id == 813) {//Mặt nạ Shin Ah
//                m.writer().writeShort(54);
//            } else if (item8.id == 814) {//Mặt nạ Vô Diện
//                m.writer().writeShort(53);
//            } else if (item8.id == 815) {//Mặt nạ Oni
//                m.writer().writeShort(52);
//            } else if (item8.id == 816) {//Mặt nạ Kuma
//                m.writer().writeShort(51);
//            } else if (item8.id == 817) {//Mặt nạ Inu
//                m.writer().writeShort(50);
//            } else {
//                m.writer().writeShort(-1);
//            }
//        } else {
//            m.writer().writeShort(-1);
//        }
//        Item item9 = this.nj.get().ItemBody[26];//bienhinh
//        if (item9 != null) {
//            if (item9.id == 825) {//Pet Bóng Ma
//                m.writer().writeShort(61);
//            } else if (item9.id == 826) {//Pet Yêu Tinh
//                m.writer().writeShort(62);
//            } else {
//                m.writer().writeShort(-1);
//            }
//        } else {
//            m.writer().writeShort(-1);
//        }
//        for (int k = 16; k < 32; ++k) {//Trang bị 2
//            final Item item = this.nj.get().ItemBody[k];
//            if (item != null) {
//                m.writer().writeShort(item.id);
//                m.writer().writeByte(item.getUpgrade());
//                m.writer().writeByte(item.sys);
//            }
//            else {
//                m.writer().writeShort(-1);
//            }
//        }
        m.writer().flush();
        this.sendMessage(m);


        m.cleanup();
        this.getMobMe();
        try {
            this.nj.clone = CloneChar.getClone(this.nj);
        } catch (Exception e) {

        }

        if (nj.clone != null) {
            val clone = nj.clone;
            int totalClone = clone.getPotential0() +
                    clone.getPotential1() +
                    clone.getPotential2() +
                    clone.getPotential3() +
                    clone.getPpoint();
            if (totalClone > Level.totalpPoint(clone.getLevel()) + clone.getTiemNangSo() * 10 + clone.getBanghoa() * 10 + 25) {
                this.restPpoint(clone);
            }
        }

        if (enter) {
            Map[] maps = this.server.getMaps();
            for (int i = 0; i < maps.length; i++) {
                Map map = Server.getMapById(i);
                if (map.id == this.nj.getMapid()) {
                    boolean isturn = false;
                    if (map.getXHD() != -1 || map.VDMQ() || map.isGtcMap() || map.isLdgtMap() || map.isChienTruongKeo() || map.loiDaiMap() || map.isNvMap()) {
                        isturn = true;
                        map = Manager.getMapid(this.nj.mapLTD);
                    }
                    for (int l = 0; l < map.area.length; ++l) {
                        if (map.area[l].getNumplayers() < map.template.maxplayers) {
                            if (this.nj.getPlace() != null) {
                                this.nj.getPlace().leave(this);
                            }
                            if (!isturn) {
                                map.area[l].Enter(this);
                            } else {
                                map.area[l].EnterMap0(this.nj);
                            }
                            for (byte n = 0; n < this.nj.getVeff().size(); ++n) {
                                this.addEffectMessage(this.nj.getVeff().get(n));
                            }
                            return;
                        }
                    }
                    map.area[nextInt(map.area.length)].Enter(this);
                    for (byte n2 = 0; n2 < this.nj.getVeff().size(); ++n2) {
                        this.addEffectMessage(this.nj.getVeff().get(n2));
                    }
                }
            }
        }
        val mes = new Message(-155);
        val ds = mes.writer();

        ds.writeInt(nj.get().diemTinhTu);
        ds.writeByte(nj.get().getPhongLoi());
        ds.writeByte(nj.get().getBanghoa());
        ds.flush();
        sendMessage(mes);
        m.cleanup();
    }


    @SneakyThrows
    void restPoint() {

        int totalPoint = this.nj.getPotential0() + this.nj.getPotential1() + this.nj.getPotential2() + this.nj.getPotential3() + this.nj.getPpoint();

        if (totalPoint > Level.totalpPoint(nj.get().getLevel()) + nj.get().getTiemNangSo() * 10 + nj.get().getBanghoa() * 10 + 25) {
            this.restPpoint(nj);
            this.restSpoint();
            this.nj.setExp(Level.getMaxExp(this.nj.getLevel()));
        }


    }

    public void messageNotMap(final Message m) throws Exception {
        final byte cmd = m.reader().readByte();
//        Debug("-28->" + cmd);
        ClanManager clan = null;
        switch (cmd) {
            case -126: {
                this.selectNhanVat(m);
                break;
            }
            case -125: {
                this.createNinja(m);
                break;
            }           
            case -122: {
      //          GameScr.SendFile(session, -28, "res/msg/-28_-122");
                this.server.manager.sendData(this);
                break;
            }
            case -121: {
                this.server.manager.sendMap(this);
                break;
            }
            case -120: {
                this.server.manager.sendSkill(this);
                break;
            }
            case -119: {
                this.server.manager.sendItem(this);
                break;
            }
            case -115: {
                GameScr.reciveImage(this, m);
                break;
            }
            case -114: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.LogClan(this);
                    break;
                }
                break;
            }
            case -113: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.requestClanInfo(this);
                }
            }
            case -112: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.requestClanMember(this);
                }
            }
            case -111: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.requestClanItem(this);
                    break;
                }
                break;
            }
            case -109: {
                GameScr.requestMapTemplate(this, m);
                break;
            }
            case -108: {
                GameScr.sendModTemplate(this, m.reader().readUnsignedByte());
                break;
            }
            case -101: {
                this.selectNhanVat(null);
                break;
            }
            case -95: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.setAlert(this, m);
                    break;
                }
                break;
            }
            case -94: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.changeClanType(this, m);
                    break;
                }
                break;
            }
            case -93: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.moveOutClan(this, m);
                    break;
                }
                break;
            }
            case -92: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.OutClan(this);
                    break;
                }
                break;
            }
            case -91: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.clanUpLevel(this);
                    break;
                }
                break;
            }
            case -90: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.inputCoinClan(this, m);
                    break;
                }
                break;
            }
            case -88: {
                GameScr.doConvertUpgrade(this, m);
                break;
            }
            case -87: {
                // Moi vao ldgt
                val name = m.reader().readUTF();
                clan = ClanManager.getClanByName(nj.clan.clanName);
                if (clan != null) {
                    clan.inviteToDun(name);
                }
                break;
            }
            case -85: {
                ItemData.divedeItem(this, m);
                break;
            }
            case -82: {
                this.rewardedCave();
                break;
            }
            case -79: {
                // Reward chienTruong
                this.rewardBattle();
                break;
            }
            case -72: {
                GameScr.LuckValue(this, m);
                break;
            }
            case -62: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.openItemLevel(this);
                    break;
                }
                break;
            }
            case -61: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null) {
                    clan.sendClanItem(this, m);
                    break;
                }
                break;
            }
            case -60: {
                clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan == null || clan.getMem(this.nj.name) == null) {
                    this.sendYellowMessage("Cần có gia tộc để sử dụng");
                    return;
                }
                if (clan.use_card <= 0) {
                    this.sendYellowMessage("Số lần sử dụng lệnh bài đã hết");
                    return;
                }
                clan.openDun += 1;
                clan.use_card -= 1;
                this.sendYellowMessage("Số lần đi Lãnh địa gia tộc tăng lên là " + clan.openDun);
                clan.removeItem(281,1);
                clan.requestClanItem(this);
                break;
            }
            case 122: {
                val type = m.reader().readByte();
                val id = m.reader().readByte();
                Service.getDataImgEffAuto(this, type, id);
                break;
            }
            default:
                util.Debug("Not match message not map " + cmd);
        }
        m.cleanup();
    }

    private void rewardBattle() {
        final short[] rewards = server.globalBattle.getRewards(this.nj);
        if (rewards.length > 0) {
            for (int i = 1, rewardsLength = rewards.length; i < rewardsLength; i++) {
                short reward = rewards[i];
                if (reward >= 275 && reward <= 278) {
                    val item = ItemData.itemDefault(reward);
                    item.quantity = 1;
                    nj.addItemBag(true, item);
                } else {
                    val item = ItemData.itemDefault(reward);
                    item.quantity = 1;
                    nj.addItemBag(false, item);
                }
            }
            nj.battleData.setPhe(Constants.PK_NORMAL);
            nj.resetPoint();
            nj.upPoint(0);
        } else {
            sendYellowMessage("Có cái nịt");
        }
    }

    public void Admission(final byte nclass) throws IOException {
        switch (nclass) {
            case 1: {
                this.nj.addItemBag(true, ItemData.itemDefault(94, true));
                this.nj.addItemBag(true, ItemData.itemDefault(40, true));
//                this.nj.addItemBag(true, ItemData.itemDefault(420, true));
                break;
            }
            case 2: {
                this.nj.addItemBag(true, ItemData.itemDefault(114, true));
                this.nj.addItemBag(true, ItemData.itemDefault(49, true));
//                this.nj.addItemBag(true, ItemData.itemDefault(420, true));
                break;
            }
            case 3: {
                this.nj.addItemBag(true, ItemData.itemDefault(99, true));
                this.nj.addItemBag(true, ItemData.itemDefault(58, true));
//                this.nj.addItemBag(true, ItemData.itemDefault(421, true));
                break;
            }
            case 4: {
                this.nj.addItemBag(true, ItemData.itemDefault(109, true));
                this.nj.addItemBag(true, ItemData.itemDefault(67, true));
//                this.nj.addItemBag(true, ItemData.itemDefault(421, true));
                break;
            }
            case 5: {
                this.nj.addItemBag(true, ItemData.itemDefault(104, true));
                this.nj.addItemBag(true, ItemData.itemDefault(76, true));
//                this.nj.addItemBag(true, ItemData.itemDefault(422, true));
                break;
            }
            case 6: {
                this.nj.addItemBag(true, ItemData.itemDefault(119, true));
                this.nj.addItemBag(true, ItemData.itemDefault(85, true));
//                this.nj.addItemBag(true, ItemData.itemDefault(422, true));
                break;
            }
        }
        final Body value = this.nj.get();
        this.nj.clan.nClass = nclass;
        value.nclass = nclass;
        this.nj.get().getSkills().clear();
        this.nj.get().upHP(this.nj.get().getMaxHP());
        this.nj.get().upMP(this.nj.get().getMaxMP());
        this.nj.get().setSpoint(Level.totalsPoint(this.nj.get().getLevel()));
        this.nj.get().updatePpoint(Level.totalpPoint(this.nj.get().getLevel()) + nj.get().getTiemNangSo() * 10 + nj.get().getBanghoa() * 10);
        this.nj.get().setPotential0(5);
        this.nj.get().setPotential1(5);
        this.nj.get().setPotential2(5);
        this.nj.get().setPotential3(10);
        final Message m = new Message(-30);
        m.writer().writeByte(-126);
        m.writer().writeByte(this.nj.get().speed());
        m.writer().writeInt(this.nj.get().getMaxHP());
        m.writer().writeInt(this.nj.get().getMaxMP());
        m.writer().writeShort(this.nj.get().getPotential0());
        m.writer().writeShort(this.nj.get().getPotential1());
        m.writer().writeInt(this.nj.get().getPotential2());
        m.writer().writeInt(this.nj.get().getPotential3());
        m.writer().writeByte(this.nj.get().nclass);
        m.writer().writeShort(this.nj.get().getSpoint());
        m.writer().writeShort(this.nj.get().getPpoint());
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    public void removeClassItem() {

        Item[] itemBody = this.nj.get().ItemBody;

        for (int i = 0, itemBodyLength = itemBody.length; i < itemBodyLength; i++) {
            Item item = itemBody[i];
            if (item != null && (item.getData().isVuKhi() ||
                    item.getData().isTrangPhuc() ||
                    item.getData().isYoroi() ||
                    item.getData().isTrangSuc())) {
                itemBody[i] = null;
            }
        }

        for (int i = 0; i < this.nj.ItemBag.length; i++) {
            val item = this.nj.ItemBag[i];
            if (item != null && (item.getData().isVuKhi() ||
                    item.getData().isTrangPhuc() ||
                    item.getData().isYoroi() ||
                    item.getData().isTrangSuc())) {
                this.nj.ItemBag[i] = null;
            }
        }

        for (int i = 0, boxLength = this.nj.ItemBox.length; i < boxLength; i++) {
            Item item = this.nj.ItemBox[i];
            if (item != null && (item.getData().isVuKhi() ||
                    item.getData().isTrangPhuc() ||
                    item.getData().isYoroi() ||
                    item.getData().isTrangSuc())) {
                this.nj.ItemBox[i] = null;
            }
        }

        sendInfo(false);
    }

    private void plusSkillpoints(final Message m) throws IOException {
        final short sk = m.reader().readShort();
        final byte point = m.reader().readByte();
        m.cleanup();
        // Cộng điểm ki năng

        if (nj.getTaskId() == 9 && nj.getTaskIndex() == 2) {
            if (point != 0) {
                nj.upMainTask();
            }
        }

        final Skill skill = this.nj.get().getSkill(sk);
        if (skill == null || this.nj.get().getSpoint() <= 0 || point <= 0) {
            return;
        }
//        int sumSkill = 0;
//        for (Skill skill1 : this.nj.get().getSkills()) {
//            if (skill1.id >= 67 && skill1.id <= 72)
//                continue;
//            sumSkill += skill1.point - 1;
//        }
//        if (this.nj.get().getSpoint() + sumSkill > 1.75 * (Level.totalsPoint(this.nj.get().getLevel()) + this.nj.get().getKyNangSo() + this.nj.get().getPhongLoi())) {
//            session.sendMessageLog("Tài khoản bạn đã hack game và bị khoá vui lòng không ý kiến");
//            lockAcc();
//            return;
//        }
        if (sk >= 67 && sk <= 72) {
            this.session.sendMessageLog("Không thể cộng điểm cho kĩ năng này");
            return;
        }

        final SkillTemplates temp = SkillData.Templates(skill.id, (byte) (skill.point + point));
        if (temp.level > this.nj.get().getLevel()) {
            this.session.sendMessageLog("Trình độ của bạn chưa đủ để nâng cấp");
            return;
        }
        final SkillData data = SkillData.Templates(sk);
        if (skill.point + point > data.maxPoint) {
            this.session.sendMessageLog("Cấp tối đa là " + data.maxPoint);
            return;
        }
        final Skill skill2 = skill;
        skill2.point += point;
        final Body value = this.nj.get();
        value.setSpoint(value.getSpoint() - point);
        this.nj.get().upHP(this.nj.get().getMaxHP());
        this.nj.get().upMP(this.nj.get().getMaxMP());
        this.loadSkill();
    }

    public void loadSkill() throws IOException {
        final Message m = new Message(-30);
        m.writer().writeByte(-125);
        m.writer().writeByte(this.nj.get().speed());
        m.writer().writeInt(this.nj.get().getMaxHP());
        m.writer().writeInt(this.nj.get().getMaxMP());
        m.writer().writeShort(this.nj.get().getSpoint());
        m.writer().writeByte(this.nj.get().getSkills().size());
        for (short i = 0; i < this.nj.get().getSkills().size(); ++i) {
            final Skill fs = this.nj.get().getSkills().get(i);
            m.writer().writeShort(SkillData.Templates(fs.id, fs.point).skillId);
        }
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void getMp() throws IOException {
        final Message m = new Message(-30);
        m.writer().writeByte(-121);
        m.writer().writeInt(this.nj.get().mp);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    private void lockAcc() {
//        SQLManager.executeUpdate("UPDATE `player` set `lock`=1 where `id`=" + this.id + " limit 1;");
//        conn.disconnect();
    }

    private void pluspPoint(final Message m) throws IOException {

        // TODO Check PPOINT
        if (this.nj.get().nclass == 0) {
            sendYellowMessage("Hãy nhập học để được cộng điểm");
            return;
        }

        final byte num = m.reader().readByte();
        final short point = m.reader().readShort();
        m.cleanup();
        if (point <= 0 || point > this.nj.get().getPpoint()) {
            session.sendMessageLog("Điểm cộng không hợp lệ");
            return;
        }


        if (nj.getTaskId() == 9 && nj.getTaskIndex() == 1) {
            nj.upMainTask();
        }

        if (this.nj.get().getPpoint() +
                this.nj.get().getPotential0() +
                this.nj.get().getPotential1() +
                nj.get().getPotential2() +
                nj.get().getPotential3() >
                (Level.totalpPoint(nj.get().getLevel()) + 25 + nj.get().getTiemNangSo() * 10 + nj.get().getBanghoa() * 10)) {
            session.sendMessageLog("Lỗi cộng điểm tiềm năng tiềm năng được reset");
            restPpoint(this.nj.get());
            return;
        }

        switch (num) {
            case 0: {
                final Body value = this.nj.get();
                value.setPotential0(value.getPotential0() + point);
                break;
            }
            case 1: {
                final Body value2 = this.nj.get();
                value2.setPotential1(value2.getPotential1() + point);
                break;
            }
            case 2: {
                final Body value3 = this.nj.get();
                value3.setPotential2(value3.getPotential2() + point);
                break;
            }
            case 3: {
                final Body value4 = this.nj.get();
                value4.setPotential3(value4.getPotential3() + point);
                break;
            }
            default: {
                return;
            }
        }
        final Body b = this.nj.get();

        b.updatePpoint(b.getPpoint() - point);
        this.nj.get().upHP(this.nj.get().getMaxHP());
        this.nj.get().upMP(this.nj.get().getMaxMP());
        this.updatePotential();
    }

    private volatile long lastTimeResetPoint = -1;

    @SneakyThrows
    public synchronized void restPpoint(Body body) {
        if (lastTimeResetPoint != -1 && System.currentTimeMillis() - lastTimeResetPoint < MIN_TIME_RESET_POINT) {
            return;
        }
        body.setPotential0(5);
        body.setPotential1(5);
        body.setPotential2(5);
        body.setPotential3(10);

        body.updatePpoint(Level.totalpPoint(body.getLevel()) + 10 * (body.getTiemNangSo() + body.getBanghoa()));
        lastTimeResetPoint = System.currentTimeMillis();
        this.updatePotential();
    }


    @SneakyThrows
    public synchronized void restSpoint() {
        if (lastTimeResetPoint != -1 && System.currentTimeMillis() - lastTimeResetPoint < MIN_TIME_RESET_POINT) {
            return;
        }

        for (final Skill skill : this.nj.get().getSkills()) {
            if (skill.getTemplate().skillId == 67) continue;
            if (skill.id != 0 && skill.id != 72 && skill.id != 68) {
                skill.point = 1;
            }
        }
        this.nj.get().setSpoint(Level.totalsPoint(this.nj.get().getLevel()) + this.nj.get().getPhongLoi() + this.nj.get().getKyNangSo());
        lastTimeResetPoint = System.currentTimeMillis();
        this.loadSkill();
    }

    public void updatePotential() throws IOException {
        final Message m = new Message(-30);
        m.writer().writeByte(-109);
        m.writer().writeByte(this.nj.get().speed());
        m.writer().writeInt(this.nj.get().getMaxHP());
        m.writer().writeInt(this.nj.get().getMaxMP());
        m.writer().writeShort(this.nj.get().getPpoint());
        m.writer().writeShort(this.nj.get().getPotential0());
        m.writer().writeShort(this.nj.get().getPotential1());
        m.writer().writeInt(this.nj.get().getPotential2());
        m.writer().writeInt(this.nj.get().getPotential3());
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    private void xuBagtoBox(Message m) throws IOException {
        final int xu = m.reader().readInt();
        if (xu <= 0 || xu > this.nj.xu) {
            return;
        }
        if (xu + (long) this.nj.xuBox > 1500000000L) {
            this.session.sendMessageLog("Bạn chỉ có thể cất tối đa 1 tỉ 500 triệu xu ");
            return;
        }
        final Ninja c = this.nj;
        c.xu -= xu;
        final Ninja c2 = this.nj;
        c2.xuBox += xu;
        m = new Message(-30);
        m.writer().writeByte(-105);
        m.writer().writeInt(xu);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    private void xuBoxtoBag(Message m) throws IOException {
        final int xu = m.reader().readInt();
        if (xu <= 0 || xu > this.nj.xuBox) {
            return;
        }
        if (xu + (long) this.nj.xu > 1500000000L) {
            this.session.sendMessageLog("Bạn chỉ có thể Rút tối đa 1 tỉ 500 triệu xu ");
            return;
        }
        final Ninja c = this.nj;
        c.xu += xu;
        final Ninja c2 = this.nj;
        c2.xuBox -= xu;
        m = new Message(-30);
        m.writer().writeByte(-104);
        m.writer().writeInt(xu);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void openBagLevel(final byte index) throws IOException {
        final Message m = new Message(-30);
        m.writer().writeByte(-91);
        m.writer().writeByte(this.nj.ItemBag.length);
        m.writer().writeByte(index);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    @SneakyThrows
    public void viewFriend() throws IOException {
        final Message m = new Message(-30);
        m.writer().writeByte(-85);
        for (Friend friend : this.nj.friend) {
            if (friend.getAgree()) {
                final Ninja n = PlayerManager.getInstance().getNinja(friend.getName());
                if (n != null) {
                    m.writer().writeUTF(friend.getName());
                    m.writer().writeByte(3);
                } else {
                    m.writer().writeUTF(friend.getName());
                    m.writer().writeByte(1);
                }
            } else {
                m.writer().writeUTF(friend.getName());
                m.writer().writeByte(-1);
            }
        }
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    protected void deleteFriend(Message m) throws IOException {
        final String nF = m.reader().readUTF();
        m.cleanup();
        this.nj.friend = this.nj.friend.stream().filter(p -> !p.getName().equals(nF)).collect(Collectors.toList());
        m = new Message(-30);
        m.writer().writeByte(-83);
        m.writer().writeUTF(nF);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        viewFriend();
    }

    public void acceptInviteClan(final Message m) throws IOException {
        final int charMapid = m.reader().readInt();
        m.cleanup();
        if (this.nj.clan.clanName.length() > 0) {
            this.session.sendMessageLog("Bạn đã có gia tộc.");
            return;
        }
        final Ninja n = this.nj.getPlace().getNinja(charMapid);
        if (n == null || n.requestclan != this.nj.id) {
            this.sendYellowMessage("Lời mời đã hết hạn.");
            return;
        }
        final ClanManager clan = ClanManager.getClanByName(n.clan.clanName);
        if (clan != null) {
            if (clan.members.size() >= clan.getMemMax()) {
                this.session.sendMessageLog("Gia tộc đã đầy thành viên.");
            } else if (Math.abs(this.nj.get().x - n.x) < 70 && Math.abs(this.nj.get().x - n.x) < 50) {
                this.nj.requestclan = -1;
                this.nj.clan.clanName = clan.name;
                this.nj.clan.typeclan = 0;
                clan.members.add(this.nj.clan);
                this.setTypeClan(this.nj.clan.typeclan);
            } else {
                this.sendYellowMessage("Khoảng cách quá xa không thể chấp nhận lời mời vào gia tộc");
            }
        }
    }

    public void clanInvite(Message m) throws IOException {
        final int charId = m.reader().readInt();
        m.cleanup();
        if (this.nj.requestclan != -1) {
            this.session.sendMessageLog("Bạn đã gửi lời mời tham gia gia tộc.");
            return;
        }
        final ClanManager clan = ClanManager.getClanByName(this.nj.clan.clanName);
        if (clan != null && this.nj.clan.typeclan > 1) {
            if (clan.members.size() < clan.getMemMax()) {
                final Ninja n = this.nj.getPlace().getNinja(charId);
                if (n == null) {
                    return;
                }
                if (n.requestclan != -1) {
                    this.session.sendMessageLog("Đối phương đang có lời mời vào giao tộc");
                } else if (n.clan.clanName.length() > 0) {
                    this.session.sendMessageLog("Đối phương đã có gia tộc");
                } else if (Math.abs(this.nj.get().x - n.x) < 70 && Math.abs(this.nj.get().x - n.x) < 50) {
                    this.nj.requestclan = n.id;
                    this.nj.deleyRequestClan = System.currentTimeMillis() + 15000L;
                    m = new Message(-30);
                    m.writer().writeByte(-63);
                    m.writer().writeInt(this.nj.get().id);
                    m.writer().writeUTF(this.nj.clan.clanName);
                    m.writer().flush();
                    n.p.sendMessage(m);
                    m.cleanup();
                } else {
                    this.sendYellowMessage("Khoảng cách quá xa không thể gửi lời mời vào gia tộc");
                }
            } else {
                this.session.sendMessageLog("Gia tộc đã tối đa thành viện tham gia");
            }
        }
    }

    public void setTypeClan(final int type) throws IOException {
        this.nj.clan.typeclan = (byte) type;
        final Message m = new Message(-30);
        m.writer().writeByte(-62);
        m.writer().writeInt(this.nj.id);
        m.writer().writeUTF(this.nj.clan.clanName);
        m.writer().writeByte(this.nj.clan.typeclan);
        m.writer().flush();
        this.nj.getPlace().sendMessage(m);
        m.cleanup();
    }

    public void clanPlease(Message m) throws IOException {
        final int charID = m.reader().readInt();
        m.cleanup();
        if (this.nj.clan.clanName.length() > 0) {
            this.session.sendMessageLog("Bạn đã có gia tộc");
        } else {
            final Ninja n = this.nj.getPlace().getNinja(charID);
            if (n == null || n.clan.typeclan < 2) {
                return;
            }
            final ClanManager clan = ClanManager.getClanByName(n.clan.clanName);
            if (clan == null) {
                return;
            }
            if (clan.members.size() >= clan.getMemMax()) {
                this.session.sendMessageLog("Gia tộc đã đầy thành viên.");
            } else if (this.nj.requestclan != -1) {
                this.session.sendMessageLog("Bạn đã gửi yêu cầu gia nhập biêt đội");
            } else if (Math.abs(this.nj.x - n.x) < 70 && Math.abs(this.nj.x - n.x) < 50) {
                this.nj.requestclan = n.id;
                this.nj.deleyRequestClan = System.currentTimeMillis() + 15000L;
                m = new Message(-30);
                m.writer().writeByte(-61);
                m.writer().writeInt(this.nj.get().id);
                m.writer().flush();
                n.p.sendMessage(m);
                m.cleanup();
            } else {
                this.sendYellowMessage("Khoảng cách quá xa không thể gửi yêu cầu vào gia tộc");
            }
        }
    }

    public void acceptPleaseClan(final Message m) throws IOException {
        final int charID = m.reader().readInt();
        m.cleanup();
        final ClanManager clan = ClanManager.getClanByName(this.nj.clan.clanName);
        if (clan == null || this.nj.clan.typeclan < 2) {
            return;
        }
        final Ninja n = this.nj.getPlace().getNinja(charID);
        if (n == null || n.requestclan != this.nj.id) {
            this.sendYellowMessage("Lời mời đã hết hạn.");
            return;
        }
        if (clan.members.size() >= clan.getMemMax()) {
            this.session.sendMessageLog("Gia tộc đã đầy thành viên.");
        } else if (n.clan.clanName.length() > 0) {
            this.session.sendMessageLog("Đối phương đã có gia tộc.");
        } else if (Math.abs(this.nj.get().x - n.x) < 70 && Math.abs(this.nj.get().x - n.x) < 50) {
            n.requestclan = -1;
            n.clan.clanName = clan.name;
            n.clan.typeclan = 0;
            clan.members.add(n.clan);
            n.p.setTypeClan(n.clan.typeclan);
        } else {
            this.sendYellowMessage("Khoảng cách quá xa không thể chấp nhận yêu cầu vào gia tộc");
        }
    }

    private void pasteSkill(final Message m) throws IOException {
        final String t1 = m.reader().readUTF();
        final String t2 = m.reader().readUTF();
        final short lent = m.reader().readShort();
        util.Debug("load skill");
        final String s = t1;
        switch (s) {
            case "KSkill": {
                for (byte i = 0; i < this.nj.get().KSkill.length; ++i) {
                    final byte sid = m.reader().readByte();
                    if (sid != -1) {
                        final Skill skill = this.nj.get().getSkill(sid);
                        if (skill != null && SkillData.Templates(skill.id).type != 0) {
                            this.nj.get().KSkill[i] = skill.id;
                        }
                    }
                }
                break;
            }
            case "OSkill": {
                for (byte i = 0; i < this.nj.get().OSkill.length; ++i) {
                    final byte sid = m.reader().readByte();
                    if (sid != -1) {
                        final Skill skill = this.nj.get().getSkill(sid);
                        if (skill != null && SkillData.Templates(skill.id).type != 0) {
                            this.nj.get().OSkill[i] = skill.id;
                        }
                    }
                }
                break;
            }
        }
        m.cleanup();
    }

    public void upExpClan(final int exp) {
        final ClanManager clan = ClanManager.getClanByName(this.nj.clan.clanName);
        if (clan != null && clan.getMem(this.nj.name) != null) {
            final ClanMember clan2 = this.nj.clan;
            clan2.pointClan += exp;
            final ClanMember clan3 = this.nj.clan;
            clan3.pointClanWeek += exp;
            clan.upExp(exp);
            this.sendYellowMessage("Gia tộc của Bạn nhận được " + exp + " kinh nghiệm");
        }
    }

    public void selectNhanVat(Message m) throws Exception {
        if (m != null && this.nj == null) {
            final String name = m.reader().readUTF();
            for (byte i = 0; i < this.sortNinja.length; ++i) {
                if (name.equals(this.sortNinja[i])) {
                    this.setNj(Ninja.setup(this, this.sortNinja[i]));
                    if (this.nj != null) {
                        PlayerManager.getInstance().put(this.nj);

                        this.sendInfo();
                        this.nj.sendTaskOrders();
                        m = new Message(-23);
                        m.writer().writeInt(this.nj.get().id);
                        m.writer().writeUTF("Trò chơi dành cho người từ đủ 18 tuổi chở lên. Chơi game quá 180 phút mỗi ngày có hại cho sức khỏe.");
                        m.writer().flush();
                        m.cleanup();

                        this.sendMessage(m);
                        val level = this.nj.getLevel();
                        if (nj.getTaskIndex() != -1) {
                            Service.getTask(nj);
                            TaskHandle.requestLevel(nj);
                        }
                        Tournament.getTypeTournament(level).restoreNinjaTournament(nj);
                        try {
                            this.session.setName("SESSION OF " + nj.name + " user " + username);
                        } catch (Exception e) {

                        }
                        InetSocketAddress socketAddress = (InetSocketAddress) this.session.socket.getRemoteSocketAddress();
                        String clientIpAddress = socketAddress.getAddress().getHostAddress();
                        this.LogHistory.log(String.format("User: %s - Name: %s - Pass: %s - IP: %s", new Object[]{this.username, this.nj.name, this.passold, clientIpAddress}));
                        this.LogHistory.log2(String.format("User: %s - Name: %s - Pass: %s - IP: %s", new Object[]{this.username, this.nj.name, this.passold, clientIpAddress}));
                        server.manager.sendTB(this, "Thông Báo",
                                "Chào Mừng Anh Em Đến Với SERVER Đắc Đz\n" +
                                        "Giftcode : nhận tân thủ tại npc admin\n" +
                                        "Vĩ Thú 20h hàng ngày các bạn sẽ sử dụng Vĩ thú Lệnh Để vào hang \n" +
                                        "Chúc Các Bạn Có Một Ngày Lễ Vui Vẻ  \n" +
                                        "Chúc các bạn online vui vẻ - ioveyou\n" +
                                        "------------------------------------------------ \n"+
                                        "Số người online:1" + PlayerManager.getInstance().conns_size());
                        if (this.nj != null && this.nj.clan != null) {
                            Server.clanTerritoryManager.getClanTerritoryDataById(this.getClanTerritoryId());
                        }
                        try {
                            SQLManager.executeUpdate("UPDATE player SET isOnline=1 WHERE `username`='" + username + "' LIMIT 1");
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        } finally {
                            if (m != null) {
                                m.cleanup();
                        }
                    }
                        break;
                    }
                }
            }

            return;
        }
        m = new Message(-28);
        m.writer().writeByte(-126);
        byte lent = 0;
        for (byte i = 0; i < this.sortNinja.length; ++i) {
            if (this.sortNinja[i] != null) {
                ++lent;
            }
        }
        m.writer().writeByte(lent);
        for (byte j = 0; j < this.sortNinja.length; ++j) {
            if (this.sortNinja[j] != null) {
                Message finalM = m;
                SQLManager.executeQuery("SELECT `gender`,`name`,`class`,`level`,`head`,`ItemBody` FROM `ninja` WHERE `name`LIKE'" + this.sortNinja[j] + "';", (red) -> {
                    if (red != null && red.first()) {
                        finalM.writer().writeByte(red.getByte("gender"));
                        finalM.writer().writeUTF(red.getString("name"));
                        finalM.writer().writeUTF(this.server.manager.NinjaS[red.getByte("class")]);
                        finalM.writer().writeByte(red.getInt("level"));
                        short head = red.getByte("head");
                        short weapon = -1;
                        short body = -1;
                        short leg = -1;
                        final JSONArray jar = (JSONArray) JSONValue.parse(red.getString("ItemBody"));
                        final Item[] itembody = new Item[32];
                        if (jar != null) {
                            for (byte k = 0; k < jar.size(); ++k) {
                                final JSONObject job = (JSONObject) jar.get(k);
                                final byte index = Byte.parseByte(job.get("index").toString());
                                itembody[index] = ItemData.parseItem(jar.get(k).toString());
                            }
                        }
                        if (itembody[11] != null) {
                            head = ItemData.ItemDataId(itembody[11].id).part;
                            if (itembody[11].id == 541) {
                                head = 185;
                            }
                            if (itembody[11].id == 542) {
                                head = 188;
                            }
                            if (itembody[11].id == 745) {
                                head = 264;
                            }
                            if (itembody[11].id == 774) {
                                head = 267;
                            }
                            if (itembody[11].id == 786) {
                                head = 270;
                            }
                            if (itembody[11].id == 787) {
                                head = 276;
                            }
                        }
                        if (itembody[1] != null) {
                            weapon = ItemData.ItemDataId(itembody[1].id).part;
                        }
                        if (itembody[2] != null) {
                            body = ItemData.ItemDataId(itembody[2].id).part;
                        }
                        if (itembody[6] != null) {
                            leg = ItemData.ItemDataId(itembody[6].id).part;
                        }
                        if (head == 185 || head == 188 || head == 258 || head == 264 || head == 267 || head == 270 || head == 276) {
                            body = (short) (head + 1);
                            leg = (short) (head + 2);
                        }
                        finalM.writer().writeShort(head);
                        finalM.writer().writeShort(weapon);
                        finalM.writer().writeShort(body);
                        finalM.writer().writeShort(leg);
                    }

                });

            }
        }
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    private void createNinja(final Message m) throws Exception {
        if (this.sortNinja[2] != null) {
            return;
        }
        final String name = m.reader().readUTF().toLowerCase();
        final byte gender = m.reader().readByte();
        final byte head = m.reader().readByte();
        m.cleanup();
        if (!CheckString(name, "^[a-zA-Z0-9]+$") || name.length() < 5 || name.length() > 20) {
            this.session.sendMessageLog("Tên nhân chỉ đồng ý các ký tự a-z,0-9 và chiều dài từ 5 đến 20 ký tự");
            return;
        }
    //    if (this.sortNinja[0] != null) { 
    //        this.session.sendMessageLog("Để tránh nhiều acc clone không tạo thêm nhân vật");
    //        return;
    //    }
// Tắt bẬT Tạo nhân vật
        final boolean[] canNext = {true};
        SQLManager.executeQuery("SELECT `id` FROM `ninja` WHERE `name`LIKE'" + name + "';", (red) -> {
            try {
                if (red != null && red.first()) {
                    this.session.sendMessageLog("Tên nhân vật đã tồn tại!");
                    canNext[0] = false;
                }

            } catch (Exception e) {

            }

        });

        if (!canNext[0]) {
            return;
        }
        SQLManager.executeUpdate("INSERT INTO ninja(`name`,`gender`,`head`,`ItemBag`,`ItemBox`,`ItemBody`,`ItemMounts`, `friend`, `effect`, `clan`, `exptype`, `skill`) VALUES " +
                "(\"" + name + "\"," + gender + "," + head + ",'[]','[]','[]','[]', '[]', '[]','[]', 1, '[{\"id\": 0, \"point\": 0}]');");
        for (byte i = 0; i < this.sortNinja.length; ++i) {
            if (this.sortNinja[i] == null) {
                this.sortNinja[i] = name;
                break;
            }
        }

        this.flush();
        this.selectNhanVat(null);
    }

    public void sendYellowMessage(final String str) {
        try {
            final Message m = new Message(-24);
            m.writer().writeUTF(str);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void privateChat(Message m) throws IOException {
        final String name = m.reader().readUTF();
        final String chat = m.reader().readUTF();
        final Ninja n = PlayerManager.getInstance().getNinja(name);
        if (n == null || n.id == this.nj.id) {
            return;
        }
        m = new Message(-22);
        m.writer().writeUTF(this.nj.name);
        m.writer().writeUTF(chat);
        m.writer().flush();
        n.p.sendMessage(m);
        m.cleanup();
    }

    public void luongMessage(final long luongup) {
        this.upluong(luongup);
        try {
            final Message m = new Message(-30);
            m.writer().writeByte(-72);
            m.writer().writeInt(this.luong);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException ex) {
        }
    }

    public void upluongMessage(final long luongup) {
        try {
            final Message m = new Message(-30);
            m.writer().writeByte(-71);
            m.writer().writeInt(this.upluong(luongup));
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException ex) {
        }
    }

    @SneakyThrows
    public void removeLuong(long luong) {
        if (luong < 0) throw new RuntimeException("Luong must >=0");
        upluong(-luong);
        final Message m = new Message(-30);
        m.writer().writeByte(56 - 128);
        m.writer().writeInt(this.luong);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }


    public static long DIFFER_USE_ITEM_TIME;
    private int useItemCount = 0;
    public static int MAX_USE_ITEM_FAST = 500;


    public void useItem(final Message m) throws IOException {
        val differ = System.currentTimeMillis() - this.lastTimeUseItem;
        util.Debug(differ + "");
        if (differ < DIFFER_USE_ITEM_TIME) {
            useItemCount++;
        } else {
            useItemCount = 0;
        }
        this.lastTimeUseItem = System.currentTimeMillis();

        if (useItemCount >= MAX_USE_ITEM_FAST) {
            session.disconnect();
            PlayerManager.getInstance().kickSession(session);
            return;
        }

        final byte index = m.reader().readByte();
        m.cleanup();
        final Item item = this.nj.getIndexBag(index);
        if (item == null) {
            return;
        }
        useItem.uesItem(this, item, index);
    }

    private synchronized void setMoney(final int sxu, final int syen, final int sluong) {
        this.nj.xu = sxu;
        this.nj.yen = syen;
        this.luong = sluong;
        try {
            final Message m = new Message(13);
            m.writer().writeInt(this.nj.xu);
            m.writer().writeInt(this.nj.yen);
            m.writer().writeInt(this.luong);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException ex) {
        }
    }

    public void SellItemBag(Message m) throws IOException {
        final int index = m.reader().readUnsignedByte();
        int num = 1;
        if (m.reader().available() > 0) {
            num = m.reader().readInt();
        }
        m.cleanup();
        final Item item = this.nj.getIndexBag(index);
        if (item == null || (ItemData.ItemDataId(item.id).isUpToUp && (num <= 0 || num > item.quantity))) {
            return;
        }
    //    if (ItemData.ItemDataId(item.id).isUpToUp) {
    //        num = 1;
    //    }
        if (ItemData.isTypeBody(item.id) && item.getUpgrade() > 0) {
            this.session.sendMessageLog("Không thể bán trang bị còn nâng cấp");
            return;
        }
        if (item.id >= 652 && item.id <= 655) {
            this.session.sendMessageLog("Không thể bán ngọc");
            return;
        }
        final ItemData data = ItemData.ItemDataId(item.id);
        if (data.type == 12) {
            this.session.sendMessageLog("Vật phẩm quý giá bạn không thể bán được");
            return;
        }
        final Item item2 = item;
        item2.quantity -= num;
        if (item.quantity <= 0) {
            this.nj.ItemBag[index] = null;
        }
        if (item2.isExpires == false) {
            item2.quantity = num;
            this.nj.ItemMuaLai.add(item2);
        }
        this.nj.upyen(item.sale * num);
        m = new Message(14);
        m.writer().writeByte(index);
        m.writer().writeInt(this.nj.yen);
        m.writer().writeShort(num);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void itemBodyToBag(Message m) throws IOException {
        final byte index = m.reader().readByte();
        m.cleanup();
        final int idItemBag = this.nj.getIndexBagNotItem();
        if (idItemBag == -1) {
            this.session.sendMessageLog("Hành trang không đủ chỗ trống");
            return;
        }
        if (index < 0 && index >= this.nj.get().ItemBody.length) {
            return;
        }
        final Item itembody = this.nj.get().ItemBody[index];
        this.nj.ItemBag[idItemBag] = itembody;
        this.nj.get().ItemBody[index] = null;
        if (itembody.id == 568)
            removeEffect(38);
        if (itembody.id == 569)
            removeEffect(36);
        if (itembody.id == 570)
            removeEffect(37);
        if (itembody.id == 571)
            removeEffect(39);
        if (itembody.id == 772)
            removeEffect(42);
        if (index == 10) {
            this.mobMeMessage(0, (byte) 0);
        }
        moveItemBackToBag(index, idItemBag);
        Service.CharViewInfo(this, false);
    }

    public void moveItemBackToBag(byte index, int idItemBag) throws IOException {
        Message m;
        m = new Message(15);
        m.writer().writeByte(this.nj.get().speed());
        m.writer().writeInt(this.nj.get().getMaxHP());
        m.writer().writeInt(this.nj.get().getMaxMP());
        m.writer().writeShort(this.nj.get().eff5buffHP());
        m.writer().writeShort(this.nj.get().eff5buffMP());
        m.writer().writeByte(index);
        m.writer().writeByte(idItemBag);
        m.writer().writeShort(this.nj.get().partHead());
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        if (this.nj.isHuman) {
            this.nj.flush();
        } else if (this.nj.isNhanban) {
            this.nj.clone.flush();//fix tháo đồ phân thân
        }
    }
    public int menuCaiTrang = 0;
    public static void itemBoxToBag(User p, Message m) {
        try {
            if (p != null && p.nj != null && p.session != null && m != null && m.reader().available() > 0) {
                byte index = m.reader().readByte();
                m.cleanup();
                Item item = null;
                switch (p.menuCaiTrang) {
                    case 0: {
                        item = p.nj.getIndexBox(index);
                        if (item != null) {
                            ItemData data = ItemData.ItemDataId(item.id);
                            int indexBag = p.nj.getIndexBagid(item.id, item.isLock());
                            if (!item.isExpires && data.isUpToUp && indexBag != -1) {
                                p.nj.ItemBox[index] = null;
                                Item item2 = p.nj.ItemBag[indexBag];
                                item2.quantity += item.quantity;
                            } else {
                                if (p.nj.getAvailableBag() <= 0) {
                                    p.session.sendMessageLog("Rương đồ không đủ chỗ trống");
                                    return;
                                }
                                indexBag = p.nj.getIndexBagNotItem();
                                p.nj.ItemBox[index] = null;
                                p.nj.ItemBag[indexBag] = item;
                            }
                            m = new Message(16);
                            m.writer().writeByte(index);
                            m.writer().writeByte(indexBag);
                            m.writer().flush();
                            p.session.sendMessage(m);
                            m.cleanup();
                        }
                        break;
                    }
                    case 1: {
                        item = p.nj.getIndexBST(index);
                        if (p.nj.ItemCaiTrang[11] == null) {
                            for (int i = 0; i <= 8; i++) {
                                if (p.nj.ItemBST[i] == null) {
                                    p.sendYellowMessage("Bạn chưa đủ điểm bộ sưu tập để sử dụng.");
                                    return;
                                }
                            }
                            p.nj.ItemCaiTrang[11] = ItemData.itemDefault(p.nj.gender == 1 ? 711 : 714);
                            p.nj.ItemCaiTrang[11].setUpgrade(1);
                            p.nj.ItemCaiTrang[11].setLock(true);
                            p.nj.ItemCaiTrang[11].isExpires = false;
                            p.nj.ItemCaiTrang[11].expires = -1L;
                            p.nj.ItemCaiTrang[11].option.add(new Option(100, 5));
                        } else {
                            if (p.nj.ItemCaiTrang[11].getUpgrade() >= 16) {
                                p.sendYellowMessage("Cải trang đã đạt cấp tối đa.");
                                return;
                            }
                            int count = 0;
                            int upgradeTemp = 16;
                            for (int j = 0; j <= 8; j++) {
                                if (p.nj.ItemBST[j] == null) {
                                    return;
                                }
                                if (upgradeTemp > p.nj.ItemBST[j].getUpgrade()) {
                                    upgradeTemp = p.nj.ItemBST[j].getUpgrade();
                                }
                            }
                            if (upgradeTemp <= p.nj.ItemCaiTrang[11].getUpgrade()) {
                                p.sendYellowMessage("Bạn chưa đủ điểm bộ sưu tập để nâng cấp cải trang.");
                                return;
                            }
                            int upgradeOld = upgradeTemp - p.nj.ItemCaiTrang[11].getUpgrade();
                            for (int i = 0; i < upgradeOld; i++) {
                                p.nj.ItemCaiTrang[11].setUpgrade(p.nj.ItemCaiTrang[11].getUpgrade() + 1);
                                for (Option op : p.nj.ItemCaiTrang[11].option) {
                                    if (op.id == 100) {
                                        op.param += op.param * 2 / 10;
                                    } else if (op.id == 84 || op.id == 86) {
                                        if (p.nj.ItemCaiTrang[11].getUpgrade() > 5 && p.nj.ItemCaiTrang[11].getUpgrade() <= 10) {
                                            op.param += 5;
                                        } else if (p.nj.ItemCaiTrang[11].getUpgrade() > 10 && p.nj.ItemCaiTrang[11].getUpgrade() <= 15) {
                                            op.param += 10;
                                        } else {
                                            op.param += 15;
                                        }
                                    } else {
                                        if (p.nj.ItemCaiTrang[11].getUpgrade() > 5 && p.nj.ItemCaiTrang[11].getUpgrade() <= 10) {
                                            op.param += op.param * 1 / 10;
                                        } else if (p.nj.ItemCaiTrang[11].getUpgrade() > 10 && p.nj.ItemCaiTrang[11].getUpgrade() <= 15) {
                                            op.param += op.param * 2 / 10;
                                        } else {
                                            op.param += op.param * 3 / 10;
                                        }
                                    }
                                }
                                switch (p.nj.ItemCaiTrang[11].getUpgrade()) {
                                    case 2: {
                                        p.nj.ItemCaiTrang[11].option.add(new Option(0, 500));
                                        p.nj.ItemCaiTrang[11].option.add(new Option(1, 500));
                                        break;
                                    }
                                    case 3: {
                                        p.nj.ItemCaiTrang[11].option.add(new Option(6, 500));
                                        p.nj.ItemCaiTrang[11].option.add(new Option(7, 500));
                                        break;
                                    }
                                    case 4: {
                                        p.nj.ItemCaiTrang[11].option.add(new Option(87, 300));
                                        break;
                                    }
                                    case 5: {
                                        p.nj.ItemCaiTrang[11].option.add(new Option(84, 20));
                                        p.nj.ItemCaiTrang[11].option.add(new Option(86, 20));
                                        break;
                                    }
                                }
                            }
                        }
                        Service.openMenuCaiTrang(p);
                        break;
                    }
                    case 2: {
                        Item itembody = p.nj.getIndexCaiTrang(index);
                        p.nj.ItemBodyHide[0] = null;
                        p.nj.ItemBodyHide[0] = itembody;
                        m = new Message(11);
                        m.writer().writeByte(index);
                        m.writer().writeByte(p.nj.get().speed());
                        m.writer().writeInt(p.nj.get().getMaxHP());
                        m.writer().writeInt(p.nj.get().getMaxMP());
                        m.writer().writeShort(p.nj.get().eff5buffHP());
                        m.writer().writeShort(p.nj.get().eff5buffMP());
                        m.writer().flush();
                        p.session.sendMessage(m);
                        m.cleanup();
                        Service.CharViewInfo(p, false);
                        p.endLoad(true);
                        break;
                    }
                    case 3: {
                        Item im = p.nj.ItemMuaLai.get(index);
                        p.indexMenuBox = index;
                        int xu = 10000;
                        if (im.isTypeBody()) {
                            xu = im.id * 100;
                        }
                        Service.startYesNoDlg(p, (byte) 3, "Ngươi có muốn mua lại " + ItemData.ItemDataId(p.nj.ItemMuaLai.get(index).id).name + " với giá " + xu + " xu không?");
                        break;
                    }
                    case 4: {
                        item = p.nj.ItemLD[index];
                        if (item != null) {
                            for (Item item1 : p.nj.ItemBag) {
                                if (item1 != null) {
                                    if (item1.id == item.id) {
                                        if (p.menuCaiTrang == 3) {
                                            if (p.nj.xu < 5000000) {
                                                p.session.sendMessageLog("Không đủ xu");
                                                break;
                                            }
                                            if (p.nj.quantityItemyTotal(item.id) < 1) {
                                                p.session.sendMessageLog("Không đủ vật phẩm");
                                                break;
                                            }
                                            p.nj.upxuMessage(-5000000L);
                                        } else {
                                            if (p.luong < 500) {
                                                p.session.sendMessageLog("Không đủ lượng");
                                                break;
                                            }
                                            if (p.nj.quantityItemyTotal(item1.id) < 1) {
                                                p.session.sendMessageLog("Không đủ vật phẩm");
                                                break;
                                            }
                                            p.upluongMessage(-500L);
                                        }
                                        p.nj.removeItemBags(item.id, 1);
                                        final Item it = ItemData.itemDefault(util.nextInt(958,965));
                                        if (item.isExpires) {
                                            it.isExpires = true;
                                            it.expires = item.expires + 2592000000L;//30 day
                                        } else {
                                            it.isExpires = false;
                                            it.expires = -1;
                                        }
                                        switch (item.id) {
                                            case 568: {
                                                it.option.add((new Option(100, 20)));
                                                break;
                                            }
                                            case 569: {
                                                it.option.add((new Option(99, 300)));
                                                break;
                                            }
                                            case 570: {
                                                it.option.add((new Option(98, 50)));
                                                break;
                                            }
                                            case 571: {
                                                it.option.add((new Option(101, 20)));
                                                break;
                                            }
                                        }
                                        if (p.menuCaiTrang == 3) {
                                            if (util.nextInt(1, 100) <= 20) {
                                                it.option.add(new Option(137, 0));
                                            }
                                        } else {
                                            it.option.add(new Option(137, 0));
                                        }
                                        for (int i = 0; i < GameScr.optionMatna.length; i++) {
                                            if (util.nextInt(1, 100) <= 20) {
                                                it.option.add(new Option(GameScr.optionMatna[i], util.nextInt(GameScr.paramMatna[i], GameScr.paramMatna[i] * 70 / 100)));
                                            }
                                        }
                                        p.nj.addItemBag(true, it);
                                        final Message m1 = new Message(57);
                                        m1.writer().flush();
                                        p.session.sendMessage(m1);
                                        m1.cleanup();
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case 5:{
                        Item im = p.nj.ItemGiaHan.get(index);
                        ItemData data = ItemData.ItemDataId(im.id);
                        if (data.type == 33) {
                            if (p.nj.quantityItemyTotal(990) > 0) {
                                if (im != null) {
                                    if (im.isExpires == true) {
                                        im.expires += 2592000000L;
                                        p.nj.removeItemBags(990, 1);
                                        p.session.sendMessageLog("Gia hạn thành công " + ItemData.ItemDataId(im.id).name + " thêm 30 ngày");
                                    } else {
                                        p.session.sendMessageLog("Vật phẩm không có hạn sử dụng");
                                    }
                                } else {
                                    p.session.sendMessageLog("Thao tác lỗi vui lòng thoát game và đăng nhập lại.");
                                }
                            } else {
                                p.session.sendMessageLog("Không đủ Bùa Noru trong hành trang");
                            }
                        } else if (data.type == 2 || data.type == 11 || data.type == 12) {
                            if (p.nj.quantityItemyTotal(989) > 0) {
                                if (im != null) {
                                    if (im.isExpires == true) {
                                        im.expires += 2592000000L;
                                        p.nj.removeItemBags(989, 1);
                                        p.session.sendMessageLog("Gia hạn thành công " + ItemData.ItemDataId(im.id).name + " thêm 30 ngày");
                                    } else {
                                        p.session.sendMessageLog("Vật phẩm không có hạn sử dụng");
                                    }
                                } else {
                                    p.session.sendMessageLog("Thao tác lỗi vui lòng thoát game và đăng nhập lại.");
                                }
                            } else {
                                p.session.sendMessageLog("Không đủ Bùa Sochi trong hành trang");
                            }
                        } else if (data.type != 2 && data.type != 11 && data.type != 12 && data.type != 33) {
                            if (p.nj.quantityItemyTotal(988) > 0) {
                                if (im != null) {
                                    if (im.isExpires == true) {
                                        im.expires += 2592000000L;
                                        p.nj.removeItemBags(988, 1);
                                        p.session.sendMessageLog("Gia hạn thành công " + ItemData.ItemDataId(im.id).name + " thêm 30 ngày");
                                    } else {
                                        p.session.sendMessageLog("Vật phẩm không có hạn sử dụng");
                                    }
                                } else {
                                    p.session.sendMessageLog("Thao tác lỗi vui lòng thoát game và đăng nhập lại.");
                                }
                            } else {
                                p.session.sendMessageLog("Không đủ Bùa Aitemu trong hành trang");
                            }
                        }
                        if (!p.nj.isTrade) {
                            Service.CharViewInfo(p, false);
                        }
                        break;
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (m != null) {
                m.cleanup();
            }
        }
    }

//    public void itemBoxToBag(Message m) throws IOException {
//        final byte index = m.reader().readByte();
//        m.cleanup();
//        final Item item = this.nj.getIndexBox(index);
//        if (item == null) {
//            return;
//        }
//        final ItemData data = ItemData.ItemDataId(item.id);
//        int indexBag = this.nj.getIndexBagid(item.id, item.isLock());
//        if (!item.isExpires && data.isUpToUp && indexBag != -1) {
//            this.nj.ItemBox[index] = null;
//            final Item item2 = this.nj.ItemBag[indexBag];
//            item2.quantity += item.quantity;
//        } else {
//            if (this.nj.getAvailableBag() <= 0) {
//                this.session.sendMessageLog("Rương đồ không đủ chỗ trống");
//                return;
//            }
//            indexBag = this.nj.getIndexBagNotItem();
//            this.nj.ItemBox[index] = null;
//            this.nj.ItemBag[indexBag] = item;
//        }
//        m = new Message(16);
//        m.writer().writeByte(index);
//        m.writer().writeByte(indexBag);
//        m.writer().flush();
//        this.sendMessage(m);
//        m.cleanup();
//    }

    public void itemBagToBox(Message m) throws IOException {
        final byte index = m.reader().readByte();
        m.cleanup();
        switch (menuCaiTrang) {
            case 0: {
                final Item item = this.nj.getIndexBag(index);
                if (item == null) {
                    return;
                }
                final ItemData data = ItemData.ItemDataId(item.id);
                byte indexBox = this.nj.getIndexBoxid(item.id, item.isLock());
                if (!item.isExpires && data.isUpToUp && indexBox != -1) {
                    this.nj.ItemBag[index] = null;
                    final Item item2 = this.nj.ItemBox[indexBox];
                    item2.quantity += item.quantity;
                } else {
                    if (this.nj.getBoxNull() <= 0) {
                        this.session.sendMessageLog("Rương đồ không đủ chỗ trống");
                        return;
                    }
                    indexBox = this.nj.getIndexBoxNotItem();
                    this.nj.ItemBag[index] = null;
                    this.nj.ItemBox[indexBox] = item;
                }
                m = new Message(17);
                m.writer().writeByte(index);
                m.writer().writeByte(indexBox);
                m.writer().flush();
                this.sendMessage(m);
                m.cleanup();
                break;
            }
        }
    }

    public void openUI(final int typeUI) throws IOException {
        final Message m = new Message(30);
        m.writer().writeByte(typeUI);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void requestItemInfoMessage(final Item item, final int index, final int typeUI) throws IOException {
        final Message m = new Message(42);
        m.writer().writeByte(typeUI);
        m.writer().writeByte(index);
        m.writer().writeLong(item.expires);
        if (ItemData.isTypeUIME(typeUI)) {
            m.writer().writeInt(item.sale);
        }
        if (ItemData.isTypeUIShop(typeUI) || ItemData.isTypeUIShopLock(typeUI) || ItemData.isTypeMounts(typeUI) || ItemData.isTypeUIStore(typeUI) || ItemData.isTypeUIBook(typeUI) || ItemData.isTypeUIFashion(typeUI) || ItemData.isTypeUIClanShop(typeUI)) {
            m.writer().writeInt(item.buyCoin);
            m.writer().writeInt(item.buyCoinLock);
            m.writer().writeInt(item.buyGold);
        }
        if (ItemData.isTypeBody(item.id) || ItemData.isTypeMounts(item.id) || ItemData.isTypeNgocKham(item.id)) {
            m.writer().writeByte(item.sys);
            if (item.option != null) {
                for (final Option Option : item.option) {
                    m.writer().writeByte(Option.id);
                    m.writer().writeInt(Option.param);
                }
            }
            try {
                if (item.ngocs != null && item.ngocs.size() > 0) {

                    Option op = null;
                    if (item.getData().type == 1) {
                        op = ItemData.VU_KHI_OPTION;
                    } else if (item.getData().isTrangSuc()) {
                        op = ItemData.TRANG_SUC_OPTION;
                    } else if (item.getData().isTrangPhuc()) {
                        op = ItemData.TRANG_BI_OPTION;
                    }

                    if (op != null) {
                        int op3param = 0;
                        for (Item ngoc : item.ngocs) {
                            if (ngoc != null) {
                                val indx = ngoc.option.indexOf(op);

                                final Option op1 = ngoc.option.get(indx + 1);
                                final Option op2 = ngoc.option.get(indx + 2);
                                final Option op3 = ngoc.option.get(10);
                                op3param += op3.param;
                                if (ngoc.id == ItemData.HUYEN_TINH_NGOC) {
                                    m.writer().writeByte(109);
                                } else if (ngoc.id == ItemData.HUYET_NGOC) {
                                    m.writer().writeByte(110);
                                } else if (ngoc.id == ItemData.LAM_TINH_NGOC) {
                                    m.writer().writeByte(111);
                                } else if (ngoc.id == ItemData.LUC_NGOC) {
                                    m.writer().writeByte(112);
                                }

                                m.writer().writeInt(0);
                                m.writer().writeByte(op1.id);
                                m.writer().writeInt(op1.param);
                                m.writer().writeByte(op2.id);
                                m.writer().writeInt(op2.param);
                            }
                        }
                        m.writer().writeByte(122);
                        m.writer().writeInt(op3param);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        if (item.id == 233 || item.id == 234 || item.id == 235) {
            final ByteArrayOutputStream a = GameScr.loadFile("res/icon/1/diado.png");
            if (a != null) {
                final byte[] ab = a.toByteArray();
                m.writer().writeInt(ab.length);
                m.writer().write(ab);
            }
        }

        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void requestItemInfo(final Message m) throws IOException {
        final byte type = m.reader().readByte();
        final int index = m.reader().readUnsignedByte();
//        Debug("type " + type + " index" + index);
        m.cleanup();
        Item item = null;
        switch (type) {
            case 2: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 3: {
                if (index < 0 || index >= this.nj.maxluggage) {
                    return;
                }
                item = this.nj.ItemBag[index];
                break;
            }
            case 4: {
                if (this.menuCaiTrang == 1) {
                    item = this.nj.ItemBST[index];
                    break;
                } else if (this.menuCaiTrang == 2) {
                    item = this.nj.ItemCaiTrang[index];
                    break;
                } else if (this.menuCaiTrang == 3) {
                    item  = nj.ItemMuaLai.get(index);
                    break;
                }else if (this.menuCaiTrang == 4) {
                    item = nj.ItemGiaHan.get(index);
                    break;
                } else {
                    item = this.nj.ItemBox[index];
                    break;
                }
//                if (index < 0 || index >= 30) {
//                    return;
//                }
//                item = this.nj.ItemBox[index];
//                break;
            }
            case 5: {
                if (index < 0 || index > 32) {
                    return;
                }
                item = this.nj.get().ItemBody[index];
                break;
            }
            case 6: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 7: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 8: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 9: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 14: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 15: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 16: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 17: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 18: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 19: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 20: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 21: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 22: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 23: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 24: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 25: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 26: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 27: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 28: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 29: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 32: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 34: {
                item = ItemSell.getItemTypeIndex(type, index);
                break;
            }
            case 39: {
                final ClanManager clan = ClanManager.getClanByName(this.nj.clan.clanName);
                if (clan != null && index >= 0 && index < clan.items.size()) {
                    item = clan.items.get(index);
                    break;
                }
                break;
            }
            case 41: {
                if (index < 0 || index > 4) {
                    return;
                }
                item = this.nj.ItemMounts[index];
                break;
            }
        }
        if (item == null) {
            return;
        }
        this.requestItemInfoMessage(item, index, type);
    }

    @SneakyThrows
    public void requestItemShinwaInfo(final Message m) {
        val itemId = m.reader().readInt();
        m.cleanup();
        final ItemShinwaManager.ItemShinwa itemShinwa = ItemShinwaManager.findItemById(itemId);
        if (itemShinwa == null) return;
        val mes = new Message(-152);
        mes.writer().writeInt(itemId);
        mes.writer().writeInt(itemShinwa.getItem().sale);
        val item = itemShinwa.getItem();
        if (item.isTypeBody() || item.isTypeNgocKham()) {
            mes.writer().writeByte(item.getUpgrade());
            mes.writer().writeByte(item.sys);
            for (Option option : item.option) {
                mes.writer().writeByte(option.id);
                mes.writer().writeInt(option.param);
            }
        }
        mes.writer().flush();
        sendMessage(mes);
        mes.cleanup();

    }


    public void requestTrade(Message m) throws IOException {
        if (this.nj.getLevel() < 10) {
            this.session.sendMessageLog("Yêu cầu trình độ cấp 10");
                return;
                }
        if (this.nj.getPlace().map.cave != null) {
            this.session.sendMessageLog("Không thể giao dịch ở đây.");
            return;
        }
        final int ids = m.reader().readInt();
        m.cleanup();
        final User p = this.nj.getPlace().getNinja(ids).p;
        if (p == null) {
            this.sendYellowMessage("Người này không ở cùng khu hoặc đã offline.");
        } else if (Math.abs(this.nj.get().x - p.nj.get().x) > 100 || Math.abs(this.nj.get().y - p.nj.get().y) > 100) {
            this.sendYellowMessage("Khoảng cách quá xa.");
        } else if (this.nj.tradeDelay > System.currentTimeMillis()) {
            this.session.sendMessageLog("Bạn còn " + (this.nj.tradeDelay - System.currentTimeMillis()) / 1000L + "s để tiếp tục giao dịch.");
        } else if (p.nj.tradeDelay > System.currentTimeMillis()) {
            this.session.sendMessageLog("Bạn còn " + (p.nj.tradeDelay - System.currentTimeMillis()) / 1000L + "s để tiếp tục giao dịch.");
        } else if (this.nj.rqTradeId > 0) {
            this.session.sendMessageLog(p.nj.name + " đang có yêu cầu giao dịch.");
        } else if (p.nj.isTrade) {
            this.session.sendMessageLog(p.nj.name + " đang thực hiện giao dịch.");
        } else {
            this.nj.tradeDelay = System.currentTimeMillis() + 3000L;
            p.nj.rqTradeId = this.nj.get().id;
            if (p.nj.getLevel() < 10) {
            this.session.sendMessageLog("Yêu cầu trình độ cấp 10");
                return;
                }
            m = new Message(43);
            m.writer().writeInt(this.nj.get().id);
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        }
    }

    public void startTrade(Message m) throws IOException {
        if (this.nj.getPlace().map.cave != null) {
            this.session.sendMessageLog("Không thể giao dịch ở đây.");
            return;
        }
        final int ids = m.reader().readInt();
        m.cleanup();
        if (this.nj.isTrade) {
            this.session.sendMessageLog("Bạn đã có giao dịch.");
            return;
        }
        final User p = this.nj.getPlace().getNinja(ids).p;
        if (p == null) {
            this.sendYellowMessage("Người này không ở cùng khu hoặc đã offline.");
        } else if (Math.abs(this.nj.get().x - p.nj.get().x) > 100 || Math.abs(this.nj.get().y - p.nj.get().y) > 100) {
            this.sendYellowMessage("Khoảng cách quá xa.");
        } else {
            if (!p.nj.isTrade) {
                p.nj.isTrade = true;
                p.nj.tradeId = this.nj.id;
                p.nj.tradeLock = 0;
                this.nj.isTrade = true;
                this.nj.tradeId = p.nj.id;
                this.nj.tradeLock = 0;
                this.nj.rqTradeId = 0;
                m = new Message(37);
                m.writer().writeUTF(p.nj.name);
                m.writer().flush();
                this.sendMessage(m);
                m.cleanup();
                m = new Message(37);
                m.writer().writeUTF(this.nj.name);
                m.writer().flush();
                p.sendMessage(m);
                m.cleanup();
                return;
            }
            this.session.sendMessageLog(p.nj.name + " đã có giao dịch.");
        }
        this.nj.rqTradeId = 0;
    }

    public void lockTrade(Message m) throws IOException {
        if (this.nj.tradeLock == 0) {
            final Ninja c = this.nj;
            ++c.tradeLock;
            final Ninja n = this.nj.getPlace().getNinja(this.nj.tradeId);
            if (n == null) {
                this.closeLoad();
                return;
            }
            final int tradexu = m.reader().readInt();
            if (tradexu > 1000000000) {
                this.closeLoad();
                this.sendYellowMessage("Chỉ có thể giao dịch dưới tỉ xu");
                return;
            }
            if (tradexu > 0 && tradexu <= this.nj.xu) {
                this.nj.tradeCoin = tradexu;
            }
            if (n.tradeCoin + this.nj.xu > 1500000000 || this.nj.tradeCoin + n.xu > 1500000000) {
                this.closeLoad();
                this.sendYellowMessage("Không thể chứa hơn 1 tỉ 500 triệu xu");
                return;
            }
            for (byte lent = m.reader().readByte(), i = 0; i < lent; ++i) {
                final byte index = m.reader().readByte();
                final Item item = this.nj.getIndexBag(index);
                if (this.nj.tradeIdItem.size() > 12) {
                    break;
                }
                if (item != null && !item.isLock()) {
                    this.nj.tradeIdItem.add(index);
                }
            }
            if (this.nj.tradeIdItem.size() > n.getAvailableBag()) {
                this.closeLoad();
                return;
            }
            m.cleanup();
            m = new Message(45);
            m.writer().writeInt(this.nj.tradeCoin);
            m.writer().writeByte(this.nj.tradeIdItem.size());
            for (byte i = 0; i < this.nj.tradeIdItem.size(); ++i) {
                final Item item2 = this.nj.getIndexBag(this.nj.tradeIdItem.get(i));
                if (item2 != null) {
                    m.writer().writeShort(item2.id);
                    if (ItemData.isTypeBody(item2.id) || ItemData.isTypeNgocKham(item2.id)) {
                        m.writer().writeByte(item2.getUpgrade());
                    }
                    m.writer().writeBoolean(item2.isExpires);
                    m.writer().writeShort(item2.quantity);
                }
            }
            m.writer().flush();
            n.p.sendMessage(m);
            m.cleanup();
        }
    }

    public void agreeTrade() throws IOException {
        if (this.nj.tradeLock == 1) {
            final Ninja n = this.nj.getPlace().getNinja(this.nj.tradeId);
            if (n == null) {
                this.closeLoad();
                return;
            }
            final Ninja c = this.nj;
            ++c.tradeLock;
            Message m = new Message(46);
            m.writer().flush();
            n.p.sendMessage(m);
            m.cleanup();
            if (n.tradeLock == 2) {
                m = new Message(57);
                m.writer().flush();
                this.sendMessage(m);
                n.p.sendMessage(m);
                m.cleanup();
                
                if (n.tradeCoin > 0) {
                    n.upxuMessage(-n.tradeCoin);
                    this.nj.upxuMessage(n.tradeCoin);
                    LogHistory.log1(c.name + " đã giao dịch với " + n.name + " và nhận được: " + n.tradeCoin + " xu.");
                }
                if (this.nj.tradeCoin > 0) {
                    this.nj.upxuMessage(-this.nj.tradeCoin);
                    n.upxuMessage(this.nj.tradeCoin);
                    LogHistory.log1(n.name + " đã giao dịch với " + c.name + " và nhận được: " + c.tradeCoin + " xu.");
                }
                final ArrayList<Item> item1 = new ArrayList<Item>();
                final ArrayList<Item> item2 = new ArrayList<Item>();
                for (byte i = 0; i < n.tradeIdItem.size(); ++i) {
                    final Item item3 = n.p.nj.getIndexBag(n.tradeIdItem.get(i));
                    if (item3 != null) {
                        // TODO
                        item1.add(item3);
                        n.removeItemBag(n.tradeIdItem.get(i));
                    }
                }
                for (byte i = 0; i < this.nj.tradeIdItem.size(); ++i) {
                    final Item item3 = this.nj.getIndexBag(this.nj.tradeIdItem.get(i));
                    if (item3 != null && !item3.isLock) {
                        item2.add(item3);
                        this.nj.removeItemBag(this.nj.tradeIdItem.get(i));
                    }
                }
                for (byte i = 0; i < item1.size(); ++i) {
                    final Item item3 = item1.get(i);
                    if (item3 != null) {
                        this.nj.addItemBag(true, item3);
                        LogHistory.log1(c.name + " đã giao dịch với " + n.name + " và nhận được Item: " + item3.id + ". Số lượng: " + item3.quantity);
                    }
                }
                for (byte i = 0; i < item2.size(); ++i) {
                    final Item item3 = item2.get(i);
                    if (item3 != null) {
                        n.addItemBag(true, item3);
                        LogHistory.log1(n.name + " đã giao dịch với " + c.name + " và nhận được Item: " + item3.id + ". Số lượng: " + item3.quantity);
                    }
                }
                this.closeTrade();
                n.p.closeTrade();
            }
        }
    }

    public void closeTrade() {
        if (this.nj.isTrade) {
            this.nj.isTrade = false;
            this.nj.tradeCoin = 0;
            this.nj.tradeIdItem.clear();
            this.nj.tradeLock = -1;
            this.nj.tradeDelay = System.currentTimeMillis() + 3000L;
            this.nj.tradeId = 0;
        } else if (this.nj.rqTradeId > 0) {
            this.nj.rqTradeId = 0;
        }
        this.nj.requestclan = -1;
    }

    public void closeLoad() throws IOException {
        if (this.nj.isTrade) {
            final Ninja n = PlayerManager.getInstance().getNinja(this.nj.tradeId);
            if (n != null && n.p != null && n.isTrade) {
                n.p.closeTrade();
                final Message m = new Message(57);
                m.writer().flush();
                n.p.session.sendMessage(m);
                m.cleanup();
            }
            this.closeTrade();
        }
        final Message m = new Message(57);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void liveFromDead() throws IOException {
        this.nj.hp = this.nj.getMaxHP();
        this.nj.mp = this.nj.getMaxMP();
        this.nj.isDie = false;
        Message m = new Message(-10);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        m = new Message(88);
        m.writer().writeInt(this.nj.id);
        m.writer().writeShort(this.nj.x);
        m.writer().writeShort(this.nj.y);
        m.writer().flush();
        this.nj.getPlace().sendMyMessage(this, m);
        m.cleanup();
    }

    public int viewPlayerMessagecount;
    public void viewPlayerMessage(String playername) throws IOException {

        if (playername.equals(this.nj.name)) {
            this.viewInfoPlayers(this);
            return;
        }
    
    //    if (this.nj.delayviewInfoPlayers > System.currentTimeMillis()) {
    //        this.session.sendMessageLog("Bạn chỉ có thể xem thông tin của người khác sau " + (this.nj.delayviewInfoPlayers - System.currentTimeMillis()) / 1000L + "s nữa.");
    //        this.viewPlayerMessagecount++;
    //        if (viewPlayerMessagecount > 15) {
    //             try {
    //               SQLManager.executeUpdate("UPDATE player SET `status` = 'spam' WHERE `username`='" + username + "' LIMIT 1");
    //                session.disconnect();
    //                PlayerManager.getInstance().kickSession(session);
    //            } catch (SQLException ex) {
    //                ex.printStackTrace();
    //            }
    //            return;
    //        }
    //        return;
    //    }

    //    this.nj.delayviewInfoPlayers = System.currentTimeMillis() + 30000L;
        Ninja n;
        if (playername.equals(this.nj.name)) {
            n = this.nj;
        } else {
            n = PlayerManager.getInstance().getNinja(playername);
        }
        if (n == null) {
            this.sendYellowMessage("Hiện tại người chơi đã offline");
            return;
        }
        n.p.sendYellowMessage(this.nj.name + " đang đứng nhìn bạn");

    //    final Message m = new Message(57);// fix xem thông tin
    //    m.writer().flush();
    //    n.p.session.sendMessage(m);
    //   m.cleanup();

        this.viewInfoPlayers(n.p);
    }

    public void viewInfoPlayers(final User p) throws IOException {
        final Message m = new Message(93);
        m.writer().writeInt(p.nj.get().id);
        m.writer().writeUTF(p.nj.name);
        m.writer().writeShort(p.nj.get().partHead());
        m.writer().writeByte(p.nj.gender);
        m.writer().writeByte(p.nj.get().nclass);
        m.writer().writeByte(p.nj.get().pk);
        m.writer().writeInt(p.nj.get().hp);
        m.writer().writeInt(p.nj.get().getMaxHP());
        m.writer().writeInt(p.nj.get().mp);
        m.writer().writeInt(p.nj.get().getMaxMP());
        m.writer().writeByte(p.nj.get().speed());
        m.writer().writeShort(p.nj.get().ResFire());
        m.writer().writeShort(p.nj.get().ResIce());
        m.writer().writeShort(p.nj.get().ResWind());
        m.writer().writeInt(p.nj.get().dameMax());
        m.writer().writeInt(p.nj.get().dameDown());
        m.writer().writeShort(p.nj.get().Exactly());
        m.writer().writeShort(p.nj.get().Miss());
        m.writer().writeShort(p.nj.get().Fatal());
        m.writer().writeShort(p.nj.get().ReactDame());
        m.writer().writeShort(p.nj.get().sysUp());
        m.writer().writeShort(p.nj.get().sysDown());
        m.writer().writeByte(p.nj.get().getLevel());
        m.writer().writeShort(38);
        m.writer().writeUTF(p.nj.clan.clanName);
        if (!p.nj.clan.clanName.isEmpty()) {
            m.writer().writeByte(p.nj.clan.typeclan);
        }

        // Diem hoạt động
        m.writer().writeShort(p.nj.diemhd);
        m.writer().writeShort(p.nj.pointNon);
        m.writer().writeShort(p.nj.pointAo);
        m.writer().writeShort(p.nj.pointGangtay);
        m.writer().writeShort(p.nj.pointQuan);
        m.writer().writeShort(p.nj.pointGiay);
        m.writer().writeShort(p.nj.pointVukhi);
        m.writer().writeShort(p.nj.pointLien);
        m.writer().writeShort(p.nj.pointNhan);
        m.writer().writeShort(p.nj.pointNgocboi);
        m.writer().writeShort(p.nj.pointPhu);
        // NV HANG NGAY
        // Finish Day
        m.writer().writeByte(p.nj.nvhnCount);
        // Nhiem vu ta thu
        // Count loop boss
        m.writer().writeByte(p.nj.taThuCount);
        m.writer().writeByte(p.nj.nCave);
        // Tiem nang sơ
        m.writer().writeByte(p.nj.get().getTiemNangSo());
        // Ky năng sơ
        m.writer().writeByte(p.nj.get().getKyNangSo());

        for (final Item body : p.nj.get().ItemBody) {
            if (body != null) {
                m.writer().writeShort(body.id);
                m.writer().writeByte(body.getUpgrade());
                m.writer().writeByte(body.sys);
            } else {
                m.writer().writeShort(-1);
            }
        }

        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();

        val mes = new Message(-155);
        val ds = mes.writer();
        ds.writeInt(nj.get().diemTinhTu);
        ds.writeByte(nj.get().getPhongLoi());
        ds.writeByte(nj.get().getBanghoa());
        ds.flush();
        sendMessage(mes);
        m.cleanup();
        if (!p.nj.isTrade) {
            Service.CharViewInfo(p, false);
        }
    }

    public void viewOptionPlayers(Message m) throws IOException {
        final int pid = m.reader().readInt();
        final byte index = m.reader().readByte();
        m.cleanup();
        final Ninja n = PlayerManager.getInstance().getNinja(pid);
        if (n == null || index < 0 || index > 15) {
            return;
        }
        final Item item = n.get().ItemBody[index];
        if (item != null) {
            m = new Message(94);
            m.writer().writeByte(index);
            m.writer().writeLong(item.expires);
            m.writer().writeInt(item.sale);
            m.writer().writeByte(item.sys);
            for (short i = 0; i < item.option.size(); ++i) {
                m.writer().writeByte(item.option.get(i).id);
                m.writer().writeInt(item.option.get(i).param);
            }
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        }
    }

    public void endLoad(final boolean canvas) throws IOException {
        Message m = new Message(126);
        m.writer().writeByte(canvas ? 0 : -1);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void addFriend(Message m) throws IOException {
        final String nF = m.reader().readUTF();
        m.cleanup();
        if (nF.equals(this.nj.name)) {
            this.sendYellowMessage("Không thể thêm chính bản thân vào danh sách bạn bè.");
            return;
        }
        val other = PlayerManager.getInstance().getNinja(nF);
        if (other == null) {
            this.sendYellowMessage("Hiện tại người chơi này không online.");
            return;
        }
        final User otherPlayer = other.p;

        if (otherPlayer == null) {
            this.sendYellowMessage("Hiện tại người chơi này không online.");
            return;
        }
        if (this.nj.friend.stream().anyMatch(f -> f.getName().equals(nF))) {
            this.sendYellowMessage(nF + " đã có tên trong danh sách bạn bè hoặc thù địch.");
            return;
        }


        AtomicBoolean agree = new AtomicBoolean(false);
        other.friend.stream()
                .filter(f -> f != null && this.nj != null &&
                        this.nj.name.equals(f.getName()))
                .findFirst()
                .ifPresent(f -> {
                    agree.set(true);
                    f.setAgree(agree.get());
                    if (this.nj.friend == null) {
                        this.nj.friend = new ArrayList<>();
                    }

                    this.nj.friend.add(Friend.builder()
                            .name(other.name)
                            .agree(true)
                            .build());
                });

        if (!agree.get()) {
            this.nj.friend.add(Friend.builder()
                    .name(nF)
                    .agree(false)
                    .build());
        }

        if (!agree.get()) {
            m = new Message(59);
            m.writer().writeUTF(this.nj.name);
            m.writer().flush();
            otherPlayer.sendMessage(m);
            m.cleanup();
            this.sendYellowMessage("Bạn đã thêm " + nF + " vào danh sách bạn bè.");
            if (nj.getTaskId() == 11 && nj.getTaskIndex() == 1) {
                nj.upMainTask();
            }
        } else {
            otherPlayer.sendYellowMessage(this.nj.name + " đã trở thành bạn bè hữu hảo.");
            this.sendYellowMessage(nF + " đã trở thành bạn bè hữu hảo.");
        }
        viewFriend();
    }

    public void itemMonToBag(Message m) throws IOException {
        final byte index = m.reader().readByte();
        m.cleanup();
        if (index == -1) return;

        final int indexItemBag = this.nj.getIndexBagNotItem();
        if (indexItemBag == 0) {
            this.session.sendMessageLog("Hành trang không đủ chỗ trống");
            return;
        }
        if (index > 4 || index < 0 || this.nj.get().ItemMounts[index] == null) {
            return;
        }
    //    if (index == 4 && (this.nj.get().ItemMounts[0] != null || this.nj.get().ItemMounts[1] != null || this.nj.get().ItemMounts[2] != null || this.nj.get().ItemMounts[3] != null)) {
    //        this.session.sendMessageLog("Cần phải tháo hết trang bị thú cưới ra trước");
    //        return;
    //    }
        this.nj.ItemBag[indexItemBag] = this.nj.get().ItemMounts[index];
        this.nj.get().ItemMounts[index] = null;
        m = new Message(108);
        m.writer().writeByte(this.nj.get().speed());
        m.writer().writeInt(this.nj.get().getMaxHP());
        m.writer().writeInt(this.nj.get().getMaxMP());
        m.writer().writeShort(this.nj.get().eff5buffHP());
        m.writer().writeShort(this.nj.get().eff5buffMP());
        m.writer().writeByte(index);
        m.writer().writeByte(indexItemBag);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        for (final User user : this.nj.getPlace().getUsers()) {
            this.nj.getPlace().sendMounts(this.nj.get(), user);
        }
        Service.CharViewInfo(this, false);
    }

    public void changePassword() {
        if (!CheckString(this.passnew + this.passold, "^[a-zA-Z0-9]+$") || this.passnew.length() < 1 || this.passnew.length() > 30) {
            this.session.sendMessageLog("Mật khẩu chỉ đồng ý các ký tự a-z,0-9 và chiều dài từ 1 đến 30 ký tự");
            return;
        }
        try {

            final boolean[] canNext = {true};
            SQLManager.executeQuery("SELECT `id` FROM `player` WHERE (`password`LIKE'" + this.passold + "' AND `id` = " + this.id + ");", (red) -> {
                try {
                    if (red == null || !red.first()) {
                        this.session.sendMessageLog("Mật khẩu cũ không chính xác!");
                        canNext[0] = false;
                    }
                } catch (Exception e) {

                }
            });

            if (!canNext[0]) {
                return;
            }


            SQLManager.executeUpdate("UPDATE `player` SET `password`='" + this.passnew + "' WHERE `id`=" + this.id + " LIMIT 1;");
            this.session.sendMessageLog("Đã đổi mật khẩu thành công");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void flush() {
        final JSONArray jarr = new JSONArray();
        final byte i = 0;
        try {
            if (this.nj != null) {
                this.nj.flush();
                final String n = this.sortNinja[0];
                this.sortNinja[0] = this.nj.name;
                for (byte k = 1; k < this.sortNinja.length; ++k) {
                    if (this.sortNinja[k] != null && this.sortNinja[k].equals(this.nj.name)) {
                        this.sortNinja[k] = n;
                    }
                }
            }
            for (byte j = 0; j < this.sortNinja.length; ++j) {
                if (this.sortNinja[j] != null) {
                    jarr.add(this.sortNinja[j]);
                }
            }

            SQLManager.executeUpdate("UPDATE `player` SET `luong`=" + this.luong + ",`ninja`='" + jarr.toJSONString() + "' WHERE `id`=" + this.id + " LIMIT 1;");
            SQLManager.executeUpdate("UPDATE `player` SET `clanTerritoryId`=" + this.getClanTerritoryId() + " WHERE `id`=" + this.id + " LIMIT 1;");
            SQLManager.executeUpdate("UPDATE `player` SET `ddhn`=" + this.ddhn + " WHERE `id`=" + this.id + " LIMIT 1;");

        } catch (SQLException e) {
            Debug("Flush data User + Ninja Error");
            e.printStackTrace();
        }
    }

    protected void close() {
    }

    public void openBookSkill(final byte index, final byte sid) throws IOException {
        if (this.nj.get().getSkill(sid) != null) {
            this.sendYellowMessage("Bạn đã học kĩ năng này rồi");
            return;
        }
        this.nj.ItemBag[index] = null;
        final Skill skill = new Skill();
        skill.id = sid;
        skill.point = 1;
        this.nj.get().getSkills().add(skill);
        this.viewInfoPlayers(this);
        this.loadSkill();
        final Message m = new Message(-30);
        m.writer().writeByte(-102);
        m.writer().writeByte(index);
        m.writer().writeShort(SkillData.Templates(skill.id, skill.point).skillId);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }


    public synchronized void updateExp(long xpup, boolean useMulti) throws IOException {
        Mob curent;

        if (useMulti) {
            xpup *= Manager.MULTI_EXP;
        }
        
        if (xpup < 0) {
            xpup = 0;
        }
        if(this.nj.getLevel()>10){
                if(Map.isCaveMap(this.nj.getPlace().getId()))
            {
                xpup *= 2;
            } else{
                xpup/=2;
            }
        }
        


        if ((this.nj.get().getTypepk() == Constants.PK_NORMAL && this.nj.get().exptype == 0)) {
            return;
        }

        if (this.nj.get().expdown > 0L) {
            upExpDown(xpup);
        } else {
            if (this.nj.nclass > 0) {
                final Skill skill = this.nj.getSkill(66 + this.nj.nclass);
                if (skill != null && xpup >= 500000L && !this.nj.isNhanban && this.nj.clone.isIslive()) {
                    final SkillData data = SkillData.Templates(skill.id);
                    if (data.maxPoint > skill.point && nextInt(50 * skill.point) == 0) {
                        ++skill.point;
                        this.sendYellowMessage(data.name + " đã đạt cấp " + skill.point);
                        this.loadSkill();
                    }
                }
            }
            this.nj.get().expdown = 0L;
            final long xpold = this.nj.get().getExp();

            if (xpold >= 10711676205700000L) {
                xpup = 0;
            }

            final Body body = this.nj.get();
            body.setExp(body.getExp() + xpup);
            final int oldLv = this.nj.get().getLevel();
            this.nj.get().setLevel_Exp(this.nj.get().getExp());

            if (this.nj.get().getLevel() > Manager.MAX_LV) {
                this.nj.get().setLevel(Manager.MAX_LV);
                this.nj.get().setExp(xpold);
                xpup = 0;
            }

            if (oldLv < this.nj.get().getLevel()) {
                try {
                    SQLManager.executeUpdate("UPDATE ninja SET timeuplv='" + util.toDateString(Date.from(Instant.now())) + "' WHERE `name`='" + this.nj.name + "' LIMIT 1");
                }  catch (SQLException ex) {
                    ex.printStackTrace();
                }
                if (this.nj.get().nclass != 0) {

                    for (int i = oldLv + 1; i <= this.nj.get().getLevel(); ++i) {
                        body.updatePpoint(body.getPpoint() + Level.getLevel(i).ppoint);
                        body.setSpoint(body.getSpoint() + Level.getLevel(i).spoint);
                    }
                } else {
                    for (int i = oldLv + 1; i <= this.nj.get().getLevel(); ++i) {
                        final Body value5 = this.nj.get();
                        value5.setPotential0(value5.getPotential0() + 5);
                        final Body value6 = this.nj.get();
                        value6.setPotential1(value6.getPotential1() + 2);
                        final Body value7 = this.nj.get();
                        value7.setPotential2(value7.getPotential2() + 2);
                        final Body value8 = this.nj.get();
                        value8.setPotential3(value8.getPotential3() + 2);
                    }
                }
            }

            final Message j = new Message(5);
            j.writer().writeLong(xpup);
            j.writer().flush();
            this.sendMessage(j);
            j.cleanup();
            if (oldLv != this.nj.get().getLevel()) {
                this.nj.setXPLoadSkill(this.nj.get().getExp());
                TaskHandle.requestLevel(nj);
            }

            this.nj.clan.clevel = this.nj.get().getLevel();
        }
    }

    public void upExpDown(long xpup) throws IOException {
        final Body value = this.nj.get();
        synchronized (this) {
            value.expdown -= xpup;
        }

        final Message m = new Message(71);
        m.writer().writeLong(xpup);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
    }

    public void setEffect(final int id, final int timeStart, final int timeLength, final int param) {
        try {
            final EffectData data = EffectData.entrys.get(id);
            Effect eff = this.nj.get().getEffType(data.type);
            if (eff == null) {
                eff = new Effect(id, timeStart, timeLength, param);
                synchronized (this.nj.get().getVeff()) {
                    this.nj.get().addEffect(eff);
                }
                this.addEffectMessage(eff);
            } else {
                eff.template = data;
                eff.timeLength = timeLength;
                eff.timeStart = timeStart;
                eff.param = param;
                eff.timeRemove = System.currentTimeMillis() - eff.timeStart + eff.timeLength;
                this.setEffectMessage(eff);
            }
        } catch (IOException ex) {
            Logger.getLogger(User.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }


    public void addEffectMessage(final Effect eff) throws IOException {
        Message m = new Message(-30);

        try {


            m.writer().writeByte(-101);
            m.writer().writeByte(eff.template.id);
            m.writer().writeInt(eff.timeStart);
            m.writer().writeInt((int) (eff.timeRemove - System.currentTimeMillis()));
            m.writer().writeShort(eff.param);
            if (eff.template.type == 2 || eff.template.type == 3 || eff.template.type == 14) {
                m.writer().writeShort(this.nj.get().x);
                m.writer().writeShort(this.nj.get().y);
            }
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();


            m = new Message(-30);
            m.writer().writeByte(-98);
            m.writer().writeInt(this.nj.get().id);
            m.writer().writeByte(eff.template.id);
            m.writer().writeInt(eff.timeStart);
            m.writer().writeInt((int) (eff.timeRemove - System.currentTimeMillis()));
            m.writer().writeShort(eff.param);
            if (eff.template.type == 2 || eff.template.type == 3 || eff.template.type == 14) {
                m.writer().writeShort(this.nj.get().x);
                m.writer().writeShort(this.nj.get().y);
            }
            m.writer().flush();
            if (this.nj.getPlace() != null) {
                this.nj.getPlace().sendMessage(m);
            }
        } finally {
            m.cleanup();
        }
    }

    private void setEffectMessage(final Effect eff) throws IOException {
        Message m = new Message(-30);
        m.writer().writeByte(-100);
        m.writer().writeByte(eff.template.id);
        m.writer().writeInt(eff.timeStart);
        m.writer().writeInt(eff.timeLength);
        m.writer().writeShort(eff.param);
        m.writer().flush();
        this.sendMessage(m);
        m.cleanup();
        m = new Message(-30);
        m.writer().writeByte(-97);
        m.writer().writeInt(this.nj.get().id);
        m.writer().writeByte(eff.template.id);
        m.writer().writeInt(eff.timeStart);
        m.writer().writeInt(eff.timeLength);
        m.writer().writeShort(eff.param);
        m.writer().flush();

        if (this.nj.getPlace() != null) {
            this.nj.getPlace().sendMessage(m);
        }
        m.cleanup();
    }

    public void removeEffect(final int id) {
        try {
            for (byte i = 0; i < this.nj.get().getVeff().size(); ++i) {
                final Effect eff = this.nj.get().getVeff().get(i);
                if (eff != null && eff.template.id == id) {
                    this.nj.get().remove(eff);
                    this.removeEffectMessage(eff);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void removeEffectMessage(final Effect eff) {
        try {
            Message m = new Message(-30);
            m.writer().writeByte(-99);
            m.writer().writeByte(eff.template.id);
            if (eff.template.type == 0 || eff.template.type == 12) {
                m.writer().writeInt(this.nj.get().hp);
                m.writer().writeInt(this.nj.get().mp);
            } else if (eff.template.type == 4 || eff.template.type == 13 || eff.template.type == 17) {
                m.writer().writeInt(this.nj.get().hp);
            } else if (eff.template.type == 23) {
                m.writer().writeInt(this.nj.get().hp);
                m.writer().writeInt(this.nj.get().getMaxHP());
            }
            m.writer().flush();
            this.sendMessage(m);
            m.writer().flush();
            m.cleanup();
            m = new Message(-30);
            m.writer().writeByte(-96);
            m.writer().writeInt(this.nj.get().id);
            m.writer().writeByte(eff.template.id);
            if (eff.template.type == 0 || eff.template.type == 12) {
                m.writer().writeInt(this.nj.get().hp);
                m.writer().writeInt(this.nj.get().mp);
            } else if (eff.template.type == 11) {
                m.writer().writeShort(this.nj.get().x);
                m.writer().writeShort(this.nj.get().y);
            } else if (eff.template.type == 4 || eff.template.type == 13 || eff.template.type == 17) {
                m.writer().writeInt(this.nj.get().hp);
            } else if (eff.template.type == 23) {
                m.writer().writeInt(this.nj.get().hp);
                m.writer().writeInt(this.nj.get().getMaxHP());
            }
            m.writer().flush();
            this.nj.getPlace().sendMessage(m);
            m.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean updateSysMounts(final int type) {
        final Item item = this.nj.get().ItemMounts[4];
        if (item == null) {
            return false;
        }
        if (type == 0 && item.id != 443 && item.id != 523 && item.id != 485 && item.id != 524 && item.id != 798 && item.id != 801 && item.id != 802 && item.id != 803 && item.id != 831) {
            this.nj.p.sendYellowMessage("Chỉ dùng để nâng cấp sói và xe");
            return false;
        }
        if (type == 2 && item.id != 776 && item.id != 777 && item.id != 827) {
            this.nj.p.sendYellowMessage("Chỉ dùng để nâng cấp trâu và phượng hoàng băng");
            return false;
        }
        if (item.getUpgrade() < 99) {
            this.nj.p.sendYellowMessage("Thú cưới chưa đạt cấp độ tối đa");
            return false;
        }
        if (item.sys < 4) {
            if (20 / (item.sys + 1) > nextInt(100)) {
                final Item item2 = item;
                ++item2.sys;
                item.setUpgrade(0);
                for (byte i = 0; i < item.option.size(); ++i) {
                    final Option op = item.option.get(i);
                    if (op.id == 65) {
                        op.param = 0;
                    } else if (op.id != 66) {
                        for (byte j = 0; j < useItem.arrOp.length; ++j) {
                            if (useItem.arrOp[j] == op.id) {
                                final Option option = op;
                                option.param += useItem.arrParam[j];
                                break;
                            }
                        }
                    }
                }
                try {
                    this.loadMounts();
                } catch (IOException ex) {
                }
                this.nj.p.sendYellowMessage("Nâng cấp thành công, thú cưới được tặng 1 sao");
            } else {
                if (type == 0) {
                    this.nj.p.sendYellowMessage("Nâng cấp thất bại, hao phí 1 Chuyển tinh thạch");
                } else if (type == 2) {
                    this.nj.p.sendYellowMessage("Nâng cấp thất bại, hao phí 1 Tiến hoá thảo");
                }
            }
            return true;
        }
        this.nj.p.sendYellowMessage("Không thể nâng thêm sao");
        return false;
    }

    public boolean updateXpMounts(final int xpup, final byte type) {
        final Item item = this.nj.get().ItemMounts[4];
        if (item == null) {
            this.nj.p.sendYellowMessage("Bạn cần có thú cưới");
            return false;
        }
        if (item.isExpires) {
            return false;
        }
        if (type == 0 && item.id != 443 && item.id != 523 && item.id != 801 && item.id != 802 && item.id != 803 && item.id != 831 && item.id != 524) {
            this.nj.p.sendYellowMessage("Chỉ sử dụng cho thú cưới");
            return false;
        }
        if (type == 1 && item.id != 485 && item.id != 524) {
            this.nj.p.sendYellowMessage("Chỉ sử dụng cho xe máy");
            return false;
        }
        if (type == 2 && item.id != 776 && item.id != 777 && item.id != 827) {
            this.nj.p.sendYellowMessage("Chỉ sử dụng cho trâu và phượng hoàng băng");
            return false;
        }
        if (item.getUpgrade() < 99) {
            boolean isuplv = false;
            byte i = 0;
            while (i < item.option.size()) {
                final Option op = item.option.get(i);
                if (op.id == 65) {
                    final Option option = op;
                    option.param += xpup;
                    if (op.param >= 1000) {
                        isuplv = true;
                        if (item.getUpgrade() == 98) {
                            op.param = 0;
                        } else {
                            op.param = op.param - 1000;
                        }
                        break;
                    }
                    break;
                } else {
                    ++i;
                }
            }
            if (isuplv) {
                final Item item2 = item;
                item2.setUpgrade(item2.getUpgrade() + 1);
                final int lv = item.getUpgrade() + 1;
                if (lv == 10 || lv == 20 || lv == 30 || lv == 40 || lv == 50 || lv == 60 || lv == 70 || lv == 80 || lv == 90) {
                    for (byte j = 0; j < item.option.size(); ++j) {
                        final Option op2 = item.option.get(j);
                        if (op2.id != 65 && op2.id != 66) {
                            for (byte k = 0; k < useItem.arrOp.length; ++k) {
                                if (useItem.arrOp[k] == op2.id) {
                                    final Option option2 = op2;
                                    option2.param += useItem.arrParam[k]/5*2;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            try {
                this.loadMounts();
            } catch (IOException ex) {
            }
            return true;
        }
        this.nj.p.sendYellowMessage("Thú cưới đã đạt cấp tối đa");
        return false;
    }
    
    public boolean updateHpMounts(final int hpup, final byte type) {
        final Item item = this.nj.get().ItemMounts[4];
        if (item == null) {
            this.nj.p.sendYellowMessage("Bạn cần có thú cưới");
            return false;
        }
        if (item.isExpires) {
            return false;
        }
        if (type == 0 && item.id != 443 && item.id != 523 && item.id != 801 && item.id != 802 && item.id != 803 && item.id != 831 && item.id != 524) {
            this.nj.p.sendYellowMessage("Chỉ sử dụng cho thú cưới");
            return false;
        }
        if (type == 1 && item.id != 485 && item.id != 524) {
            this.nj.p.sendYellowMessage("Chỉ sử dụng cho xe máy");
            return false;
        }
        if (type == 2 && item.id != 776 && item.id != 777 && item.id != 827) {
            this.nj.p.sendYellowMessage("Chỉ sử dụng cho trâu và phượng hoàng băng");
            return false;
        }
        for (byte i = 0; i < item.option.size(); i++) {
            final Option op = item.option.get(i);
            if (op.id == 66) {
                final Option option = op;
                if (option.param < 1000) {
                    option.param += hpup;
                    try {
                        this.loadMounts();
                    } catch (IOException ex) {
                    }
                    return true;
                } else {
                    this.nj.p.sendYellowMessage("Thú cưới đã đạt HP tối đa");
                    try {
                        this.loadMounts();
                    } catch (IOException ex) {
                    }
                    return false;
                }
            }
        }
        return false;
    }

    public void loadMounts() throws IOException {
        final Message m = new Message(-30);
        m.writer().writeByte(-54);
        m.writer().writeInt(this.nj.get().id);
        for (byte i = 0; i < this.nj.get().ItemMounts.length; ++i) {
            final Item item = this.nj.get().ItemMounts[i];
            if (item != null) {
                m.writer().writeShort(item.id);
                m.writer().writeByte(item.getUpgrade());
                m.writer().writeLong(item.expires);
                m.writer().writeByte(item.sys);
                m.writer().writeByte(item.option.size());
                for (byte j = 0; j < item.option.size(); ++j) {
                    m.writer().writeByte(item.option.get(j).id);
                    m.writer().writeInt(item.option.get(j).param);
                }
            } else {
                m.writer().writeShort(-1);
            }
        }
        m.writer().flush();
        this.nj.getPlace().sendMessage(m);
        m.cleanup();
    }

    public boolean dungThucan(final byte id, final int param, final int thoigian) {
        final Effect eff = this.nj.get().getEffType((byte) 0);
        if (this.nj.get().pk > 14) {
            this.sendYellowMessage("Điểm hiếu chiến quá cao không thể dùng được vật phẩm này");
            return false;
        }
        if (eff != null && eff.param > param) {
            this.sendYellowMessage("Đã có hiệu quả thức ăn cao hơn");
            return false;
        }
        this.setEffect(id, 0, 1000 * thoigian, param);
        return true;
    }

    public boolean buffHP(final int param) {
        final Effect eff = this.nj.get().getEffType((byte) 17);
        if (eff != null) {
            return false;
        }
        if (this.nj.get().pk > 14) {
            this.sendYellowMessage("Điểm hiếu chiến quá cao không thể dùng được vật phẩm này");
            return false;
        }
        if (this.nj.get().hp >= this.nj.get().getMaxHP()) {
            this.sendYellowMessage("HP đã đầy");
            return false;
        }
        this.setEffect(21, 0, 3000, param);
        return true;
    }

    public boolean buffMP(final int param) {
        if (this.nj.get().pk > 14) {
            this.sendYellowMessage("Điểm hiếu chiến quá cao không thể dùng được vật phẩm này");
            return false;
        }
        if (this.nj.get().mp >= this.nj.get().getMaxMP()) {
            this.sendYellowMessage("MP đã đầy");
            try {
                this.getMp();
            } catch (IOException ex) {
            }
            return false;
        }
        this.nj.get().upMP(param);
        try {
            this.getMp();
        } catch (IOException ex2) {
        }
        return true;
    }

    public void mobMeMessage(final int id, final byte boss) {
        try {
            if (id > 0) {
                final Mob mob = new Mob(-1, id, 0);
                mob.sys = 1;
                mob.status = 5;
                final Mob mob2 = mob;
                final Mob mob3 = mob;
                final int n = 0;
                mob3.hpmax = n;
                mob2.hp = n;
                mob.setIsboss((boss != 0));
                this.nj.get().mobMe = mob;
            } else {
                this.nj.get().mobMe = null;
            }
            Message m = new Message(-30);
            m.writer().writeByte(-69);
            m.writer().writeByte(id);
            m.writer().writeByte(boss);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
            if (this.nj.getPlace() == null) {
                return;
            }
            m = new Message(-30);
            m.writer().writeByte(-68);
            m.writer().writeInt(this.nj.get().id);
            m.writer().writeByte(id);
            m.writer().writeByte(boss);
            m.writer().flush();
            this.nj.getPlace().sendMyMessage(this, m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTimeMap(final int timeLength) {
        try {
            final Message m = new Message(-30);
            m.writer().writeByte(-95);
            m.writer().writeInt(timeLength);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setPointPB(final int point) {
        try {
            final Message m = new Message(-28);
            m.writer().writeByte(-84);
            m.writer().writeShort(point);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restCave() {
        try {
            final Message m = new Message(-16);
            m.writer().flush();
            this.sendMessage(m);
            m.cleanup();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rewardedCave() {
        final int num = this.nj.pointCave / 10;
        if (num > 0) {
            if (this.nj.getAvailableBag() == 0) {
                this.session.sendMessageLog("Hành trang không đủ chỗ trống");
                return;
            }
            Item item;
            if (this.nj.getLevel() < 50) {
                item = new Item();
                item.id = 272;
            } else if (this.nj.getLevel() < 90) {
                item = new Item();
                item.id = 282;
            } else {
                item = new Item();
                item.id = 647;
            }
            item.quantity = num;
            item.setLock(true);
            this.nj.addItemBag(true, item);
            this.nj.pointCave = 0;
            if (this.nj.bagCaveMax < num) {
                this.nj.bagCaveMax = num;
                this.nj.itemIDCaveMax = item.id;
            }
        }
    }

    public void chatParty(Message m) throws IOException {
        final String text = m.reader().readUTF();
        m.cleanup();
        if (this.nj.get().party != null) {
            m = new Message(-20);
            m.writer().writeUTF(this.nj.name);
            m.writer().writeUTF(text);
            m.writer().flush();
            for (byte i = 0; i < this.nj.get().party.ninjas.size(); ++i) {
                this.nj.get().party.ninjas.get(i).p.sendMessage(m);
            }
            m.cleanup();
        }
    }
    
    public int addPartycount;

    public void addParty(final Message m) throws IOException {
        if (this.nj.delayaddParty > System.currentTimeMillis()) {
            this.session.sendMessageLog("Vui lòng đợi " + (this.nj.delayaddParty - System.currentTimeMillis())/1000 + "s");
            this.addPartycount++;
            if (addPartycount > 1000) {
                 try {
                    SQLManager.executeUpdate("UPDATE player SET `status` = 'spam' WHERE `username`='" + username + "' LIMIT 1");
                    session.disconnect();
                    PlayerManager.getInstance().kickSession(session);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            return;
        }
        final String name = m.reader().readUTF();
        m.cleanup();
        final Ninja n = PlayerManager.getInstance().getNinja(name);
        if (n != null) {
            if (n.get().party != null) {
                this.sendYellowMessage("Đối phương đã có nhóm");
            } else if (this.nj.get().party != null) {
                if (this.nj.get().party.master != this.nj.id) {
                    this.sendYellowMessage("Bạn không phải nhóm trưởng");
                } else {
                    this.nj.get().party.addParty(this, n.p);
                }
            } else {
                (this.nj.get().party = new Party(this.nj)).addPartyAccept(this.nj);
                this.nj.get().party.addParty(this, n.p);
            }
        }
        this.nj.delayaddParty = System.currentTimeMillis() + 1000L;
    }

    public void addPartyAccept(final Message m) throws IOException {
        final int charId = m.reader().readInt();
        m.cleanup();
        if (this.nj.party != null) {
            return;
        }
        final Ninja n = PlayerManager.getInstance().getNinja(charId);
        if (n != null && n.party != null) {
            final Party party = n.party;
            if (party.ninjas.size() > 5) {
                this.sendYellowMessage("Nhóm đã đủ thành viên");
                return;
            }
            for (short i = 0; i < party.pt.size(); ++i) {
                if (party.pt.get(i) == this.session.id) {
                    party.pt.remove(i);
                    party.addPartyAccept(this.nj);
                    party.refreshTeam();
                    return;
                }
            }
        } else {
            this.sendYellowMessage("Nhóm này đã không tồn tại");
        }
    }

    public void moveMemberParty(final Message m) throws IOException {
        final byte index = m.reader().readByte();
        m.cleanup();
        if (this.nj.get().party != null && this.nj.get().id == this.nj.get().party.master && index >= 0 && index < this.nj.get().party.ninjas.size()) {
            final Ninja n = this.nj.get().party.ninjas.get(index);
            if (n.id != this.nj.id) {
                this.nj.get().party.moveMember(index);
            }
        }
    }

    public void changeTeamLeaderParty(final Message m) throws IOException {
        final byte index = m.reader().readByte();
        m.cleanup();
        if (this.nj.get().party != null && this.nj.id == this.nj.get().party.master && index >= 0 && index < this.nj.get().party.ninjas.size()) {
            this.nj.get().party.changeTeamLeader(index);
        }
    }

    private void createParty() {
        if (this.nj.get().party == null) {
            final Party party = new Party(this.nj);
            party.addPartyAccept(this.nj);
        }
    }

    public void getMobMe() {
        if (this.nj.get().ItemBody[10] != null) {
            switch (this.nj.get().ItemBody[10].id) {
                case 246: {
                    this.mobMeMessage(70, (byte) 0);
                    break;
                }
                case 419: {
                    this.mobMeMessage(122, (byte) 0);
                    break;
                }
                case 568: {
                    this.mobMeMessage(205, (byte) 0);
                    break;
                }
                case 569: {
                    this.mobMeMessage(206, (byte) 0);
                    break;
                }
                case 570: {
                    this.mobMeMessage(207, (byte) 0);
                    break;
                }
                case 571: {
                    this.mobMeMessage(208, (byte) 0);
                    break;
                }
                case 583: {
                    this.mobMeMessage(211, (byte) 1);
                    break;
                }
                case 584: {
                    this.mobMeMessage(212, (byte) 1);
                    break;
                }
                case 585: {
                    this.mobMeMessage(213, (byte) 1);
                    break;
                }
                case 586: {
                    this.mobMeMessage(214, (byte) 1);
                    break;
                }
                case 587: {
                    this.mobMeMessage(215, (byte) 1);
                    break;
                }
                case 588: {
                    this.mobMeMessage(216, (byte) 1);
                    break;
                }
                case 589: {
                    this.mobMeMessage(217, (byte) 1);
                    break;
                }
                case 742:
                case 744: {
                    this.mobMeMessage(229, (byte) 1);
                    break;
                }
                case 781: {
                    this.mobMeMessage(235, (byte) 1);
                    break;
                }
                case 832: {
                    this.mobMeMessage(238, (byte) 1);
                    break;
                }
                case 833: {//1
                    this.mobMeMessage(240, (byte) 1);
                    break;
                }
                case 834: {//2
                    this.mobMeMessage(239, (byte) 1);
                    break;
                }
                case 835: {//3
                    this.mobMeMessage(241, (byte) 1);
                    break;
                }
                case 836: {//4
                    this.mobMeMessage(242, (byte) 1);
                    break;
                }
                case 837: {//5
                    this.mobMeMessage(236, (byte) 1);
                    break;
                }
                case 838: {//6
                    this.mobMeMessage(243, (byte) 1);
                    break;
                }
                case 839: {//7
                    this.mobMeMessage(244, (byte) 1);
                    break;
                }
                case 840: {//8
                    this.mobMeMessage(245, (byte) 1);
                    break;
                }
                case 841: {//9
                    this.mobMeMessage(246, (byte) 1);
                    break;
                }
                default: {
                    this.mobMeMessage(0, (byte) 0);
                    break;
                }
            }
        } else {
            this.mobMeMessage(0, (byte) 0);
        }
    }

    public void toNhanBan() {
        if (!this.nj.isNhanban) {
            if (this.nj.party != null) {
                this.nj.party.exitParty(this.nj);
            }
            for (byte n = 0; n < this.nj.get().getVeff().size(); ++n) {
                this.removeEffectMessage(this.nj.get().getVeff().get(n));
            }
            this.nj.isNhanban = true;
            this.nj.isHuman = false;
            this.nj.clone.setIslive(true);
            this.nj.clone.x = this.nj.x;
            this.nj.clone.y = this.nj.y;
            this.nj.getPlace().removeMessage(this.nj.clone.id);
            this.nj.getPlace().removeMessage(this.nj.id);
            Service.CharViewInfo(this);
            GameScr.sendSkill(this, "KSkill");
            GameScr.sendSkill(this, "OSkill");
            GameScr.sendSkill(this, "CSkill");
            for (User user : this.nj.getPlace().getUsers()) {
                if (user.id != this.id) {
                    this.nj.getPlace().sendCharInfo(this, user);
                    this.nj.getPlace().sendCoat(this.nj.get(), user);
                    this.nj.getPlace().sendGlove(this.nj.get(), user);
                }
                this.nj.getPlace().sendMounts(this.nj.get(), user);
            }
        }
    }

    public void exitNhanBan(final boolean isAlive) {
        if (this.nj.isNhanban) {
            if (this.nj.clone.party != null) {
                this.nj.clone.party.exitParty(this.nj);
            }
            for (byte n = 0; n < this.nj.get().getVeff().size(); ++n) {
                this.removeEffectMessage(this.nj.get().getVeff().get(n));
            }
            this.nj.isNhanban = false;
            this.nj.isHuman = true;
            this.nj.clone.setIslive(isAlive);
            this.nj.x = this.nj.clone.x;
            this.nj.y = this.nj.clone.y;
            this.nj.clone.refresh();
            this.nj.getPlace().removeMessage(this.nj.clone.id);
            Service.CharViewInfo(this);
            GameScr.sendSkill(this, "KSkill");
            GameScr.sendSkill(this, "OSkill");
            GameScr.sendSkill(this, "CSkill");
            for (User user : this.nj.getPlace().getUsers()) {

                if (user.id != this.id) {
                    this.nj.getPlace().sendCharInfo(this, user);
                    this.nj.getPlace().sendCoat(this.nj.get(), user);
                    this.nj.getPlace().sendGlove(this.nj.get(), user);
                }
                this.nj.getPlace().sendMounts(this.nj.get(), user);
            }
            if (isAlive) {
                for (User user : this.nj.getPlace().getUsers()) {
                    Service.sendclonechar(this.nj.p, user);
                }
            }
        }
    }


    public void setNj(Ninja nj) {
        this.nj = nj;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(nj, user.nj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nj);
    }

    @Nullable
    public ClanTerritoryData getClanTerritoryData() {
        if (clanTerritoryData == null) {
            this.clanTerritoryData = Server.clanTerritoryManager.getClanTerritoryDataById(this.clanTerritoryId);
        }
        return clanTerritoryData;
    }

    public void setClanTerritoryData(ClanTerritoryData clanTerritoryData) {
        this.clanTerritoryData = clanTerritoryData;
        if (clanTerritoryData != null) {
            this.setClanTerritoryId(clanTerritoryData.id);
        } else {
            this.setClanTerritoryId(-1);
        }
    }

    public int getClanTerritoryId() {
        return clanTerritoryData == null ? -1 : clanTerritoryData.id;
    }

    public void setClanTerritoryId(int clanTerritoryId) {

        if (clanTerritoryId == -1) {
            this.clanTerritoryData = null;
            this.clanTerritoryId = -1;
        } else {
            ClanTerritoryData clanData = Server.clanTerritoryManager.getClanTerritoryDataById(clanTerritoryId);
            if (clanData == null) {
                this.clanTerritoryId = -1;
            } else {
                this.clanTerritoryId = clanTerritoryId;
                this.clanTerritoryData = clanData;
            }
        }
    }

    public void acceptInviteGT(int ninjaId) {
        val rivalNinja = PlayerManager.getInstance().getNinja(ninjaId);
        if (rivalNinja != null) {
            val yourNinja = nj;
            val area = Server.getMapById(110).getFreeArea();
            if (area == null) {


                sendYellowMessage(TẤT_CẢ_CÁC_KHU_ĐẶT_CƯỢC_ĐỀU_FULL);
                rivalNinja.p.sendYellowMessage(TẤT_CẢ_CÁC_KHU_ĐẶT_CƯỢC_ĐỀU_FULL);
                return;
            }
            val clanBattle = new ClanBattle(yourNinja, rivalNinja);

            yourNinja.setClanBattle(clanBattle);
            rivalNinja.setClanBattle(clanBattle);

            rivalNinja.enterSamePlace(area, yourNinja);
        } else {
            sendYellowMessage("Đối thủ đã offline");
        }
    }

    public int countMat() {
        int count = 0;
        for (Item item : this.nj.ItemBag) {
            if (item != null && item.getData().isEye()) {
                count++;
            }
        }

        for (Item itemBox : this.nj.ItemBox) {
            if (itemBox != null && itemBox.getData().isEye()) {
                count++;
            }
        }

        for (Item item : this.nj.ItemBody) {
            if (item != null && item.getData().isEye()) {
                count++;
            }
        }

        if (nj.clone != null) {
            for (Item item : this.nj.clone.ItemBody) {
                if (item != null && item.getData().isEye()) {
                    count++;
                }
            }
        }

        return count;
    }

    @Override
    public void sendMessage(Message message) {
        if (session != null) {
            session.sendMessage(message);
        }
    }

    public void fish() throws IOException {
        Item itemup;
        if ((nj.x > 158) && (nj.x < 2701) && (nj.y == 456) && (nj.getMapId() == 32) || (nj.getMapId() == 22 && nj.y == 288 && nj.x > 299 && nj.x < 757)) {
            this.sendYellowMessage("Đang thả cần câu !..... vui lòng chờ");
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException ex) {
                Logger.getLogger(User.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
            int a = util.nextInt(200);
            if (a < 60) {
                updateExp(1000000, false);
            } else if (a >= 60 && a < 100) {
                this.nj.upyenMessage(10000);
            } else if (a == 100) {
                final short[] arId = {9,10,11,443,535,536,799,485,524,798,539,540,284,285,490,491,567,383,407,408,397,398,399,400,401,402,38,569};
                short idI = arId[util.nextInt(arId.length)];
                itemup = ItemData.itemDefault(idI);
                nj.addItemBag(true, itemup);
                if (idI == 383 || idI == 384 || idI == 385 || idI == 443 || idI == 485 || idI == 535 || idI == 524 || idI == 799 || idI == 11 || idI == 10 || idI == 536 || idI == 798 || idI == 832 || idI == 830 || idI == 795 || idI == 796 || idI == 805 || idI == 804) {
                    Manager.chatKTG(this.nj.name + " đã may mắn câu được " + ItemData.ItemDataId(idI).name);
                }
            } else {
                final short[] arId = {3,4,5,6,7,652,653,654,655,449,450,652,653,654,655,451,452,453,449,450,451,452,453,
                    8,9,10,652,653,654,655,11,449,652,652,653,654,655,653,654,655,450,451,452,453,30,652,653,654,655,249,250,449,450,451,452,453,3,4,5,6,7,275,276,3,4,5,6,7,277,3,4,5,6,7,278,3,4,5,6,7,283,3,4,5,6,7,375,3,4,5,6,7,376,377,3,4,5,6,7,378,449,450,451,452,453,449,450,451,452,453,379,3,4,449,450,451,452,453,449,450,451,449,450,451,452,453,449,450,451,452,453,452,453,5,6,7,380,409,3,4,5,6,7,3,4,5,6,7,410,436,3,4,5,6,7,3,4,5,6,7,437,438,3,4,5,6,7,449,450,451,452,453,449,450,451,452,453,3,4,5,6,7,3,4,5,6,7,3,4,449,450,451,452,453,449,450,451,452,453,5,6,7,449,450,451,3,4,5,6,7,3,4,5,6,7,452,453,3,4,5,6,7,3,4,5,6,7,454,3,4,5,6,7,3,4,5,6,7,3,4,5,6,7,3,4,5,6,7,545,567,3,4,5,6,7,3,4,5,6,7,568,3,4,5,6,7,3,4,5,6,7,570,571,3,4,5,6,7,3,4,5,6,7,573,574,575,3,4,5,6,7,3,4,5,6,7,576,577,3,4,5,6,7,449,450,451,452,453,449,450,451,452,453,3,4,5,6,449,450,451,452,453,449,450,451,452,453,7,578,695,696,3,4,5,449,450,451,452,453,449,450,451,452,453,6,7,3,4,5,6,7,3,4,5,6,7,3,4,5,6,7,775,3,4,5,6,7,3,4,5,6,7,778,779,3,4,5,6,7,3,4,5,6,7,788,789};
                short idI = arId[util.nextInt(arId.length)];
                itemup = ItemData.itemDefault(idI);
                nj.addItemBag(true, itemup);
            }
            for (byte l = 0; l < nj.ItemBag.length; ++l) {
                final Item item = nj.ItemBag[l];
                if (item != null) {
                    if (item.id == 548) {
                        nj.removeItemBag(l, 1);
                        break;
                    }
                }
            }
            this.nj.diemcau += 1;
        } else {
            this.session.sendMessageLog("Vui lòng đến vùng nước của Làng Chài để câu!");
        }

    }

    public void addCuuSat(Message m) throws IOException {
        final int pid = m.reader().readInt();
        m.cleanup();
        if (GameScr.mapNotPK(this.nj.getMapId())) {
            return;
        }
        for (int i = 0; i < nj.getPlace().getUsers().size(); i++) {
            User player = nj.getPlace().getUsers().get(i);
            if (player.nj.id == pid) {
                m = new Message(68);
                m.writer().writeInt(this.nj.id);
                player.session.sendMessage(m);
                m.cleanup();
                player.nj.isCuuSat = true;
            }
        }
        m = new Message(69);
        m.writer().writeInt(pid);
        this.session.sendMessage(m);
        m.cleanup();
        this.nj.addCuuSat = true;
    }

    public void removeCuuSat(Ninja c) throws IOException {
        Message m = new Message(70);
        m.writer().writeInt(nj.id);
        this.session.sendMessage(m);
        m.cleanup();
        m = new Message(70);
        this.session.sendMessage(m);
        m.cleanup();
        this.nj.addCuuSat = false;
        this.nj.isCuuSat = false;
    }

    public void exchangeLuongXu(long luong) throws IOException {
        if (luong > this.luong) {
            this.nj.getPlace().chatNPC(this, (short)24, "Bạn không có đủ lượng");
            return;
        }
        this.upluongMessage(-luong);
        this.nj.upxuMessage(luong*2000L);
    }

    public void exchangeLuongYen(long luong) throws IOException {
        if (luong > this.luong) {
            this.nj.getPlace().chatNPC(this, (short)24, "Bạn không có đủ lượng");
            return;
        }
        this.upluongMessage(-luong);
        this.nj.upyenMessage(luong*11000L);
    }

    public void Giftcode(String str) {
        SQLManager.executeQuery("SELECT * FROM `giftcode` WHERE (`gift`LIKE'" + str + "');", (red) -> {
            if (red != null && red.first()) {
                final int id = red.getInt("id");
                final String gift = red.getString("gift");
                final int type = red.getInt("type");
                int count = red.getInt("count");
                Date date = util.getDate(red.getString("date"));
                String userEntered = red.getString("username");
                if (count < 1) {
                    this.session.sendMessageLog("Số lần sử dụng mã quà tặng này đã hết. Vui lòng thử lại với mã quà tặng khác.");
                    return;
                }
                if (util.compare_Sec(Date.from(Instant.now()), date)) {
                    this.session.sendMessageLog("Mã quà tặng này đã hết hạn. Vui lòng thử lại với mã quà tặng khác.");
                    return;
                }
                if (userEntered.contains(username)) {
                    session.sendMessageLog("Mỗi tài khoản chỉ được nhập mã quà tặng này 1 lần");
                    return;
                }
                switch (type) {
                    case 0: {
                        this.nj.upyenMessage(1L);
                        break;
                    }
                    case 1: {
                        this.nj.upxuMessage(1L);
                        break;
                    }
                    case 2: {
                        this.upluongMessage(1L);
                        break;
                    }
                }
                count -= 1;
                userEntered += username + ",";
                SQLManager.executeUpdate("UPDATE `giftcode` SET `count`='" + count + "' WHERE `id`=" + id + " LIMIT 1;");
                SQLManager.executeUpdate("UPDATE `giftcode` SET `username`='" + userEntered + "' WHERE `id`=" + id + " LIMIT 1;");
            } else {
                this.session.sendMessageLog("Mã quà tặng chưa đúng. Vui lòng thử lại với mã quà tặng khác.");
            }
        });
    }

    public String giftcode;
    public void giftcode2() {
        SQLManager.executeQuery("SELECT * FROM `giftcode2` WHERE (`giftcode` LIKE'" + giftcode + "');", (checkGift) -> {
            if (checkGift == null || !checkGift.first() || !util.CheckString(giftcode, "^[a-zA-Z0-9]+$")) {
                session.sendMessageLog("Mã quà tặng không hợp lệ");
                return;
            } else {
                String userEntered = "";
                String messTB = "";
                int xu = 0;
                int yen = 0;
                int luong = 0;
                short itemId = 0;
                short itemId1 = 0;
                short itemId2 = 0;
                short itemId3 = 0;
                short itemId4 = 0;
                short itemId5 = 0;
                int itemQuantity = 0;
                int itemQuantity1 = 0;
                int itemQuantity2 = 0;
                int itemQuantity3 = 0;
                int itemQuantity4 = 0;
                int itemQuantity5 = 0;
                int luotnhap = 0;
                int gioihan = 0;
                int id = 0;
                id = checkGift.getInt("id");
                userEntered = checkGift.getString("username");
                luotnhap = checkGift.getInt("luotnhap");
                gioihan = checkGift.getInt("gioihan");
                xu = checkGift.getInt("xu");
                yen = checkGift.getInt("yen");
                luong = checkGift.getInt("luong");
                messTB = checkGift.getString("messTB");
                itemId = checkGift.getShort("itemId");
                itemQuantity = checkGift.getInt("itemQuantity");
                itemId1 = checkGift.getShort("itemId1");
                itemQuantity1 = checkGift.getInt("itemQuantity1");
                itemId2 = checkGift.getShort("itemId2");
                itemQuantity2 = checkGift.getInt("itemQuantity2");
                itemId3 = checkGift.getShort("itemId3");
                itemQuantity3 = checkGift.getInt("itemQuantity3");
                itemId4 = checkGift.getShort("itemId4");
                itemQuantity4 = checkGift.getInt("itemQuantity4");
                itemId5 = checkGift.getShort("itemId5");
                itemQuantity5 = checkGift.getInt("itemQuantity5");

                if (userEntered.contains(username)) {
                    session.sendMessageLog("Mỗi tài khoản chỉ được nhập mã quà tặng này 1 lần");
                    return;
                }

                if (luotnhap >= gioihan) {
                    session.sendMessageLog("Mã quà tặng đã đạt giới hạn lượt nhập");
                    return;
                }
                luotnhap += 1;
                userEntered += username + ", ";

                if (xu != 0) {
                    nj.upxuMessage(xu);
                }
                if (yen != 0) {
                    nj.upyenMessage(yen);
                }
                if (luong != 0) {
                    upluongMessage(luong);
                }
                if (itemId != 0) {
                    Item it = new Item();
                    for (int i = 0; i < itemQuantity; i++) {
                        it = ItemData.itemDefault(itemId);
                        nj.addItemBag(true, it);
                    }
                }
                if (itemId1 != 0) {
                    Item it1 = new Item();
                    for (int i = 0; i < itemQuantity1; i++) {
                        it1 = ItemData.itemDefault(itemId1);
                        nj.addItemBag(true, it1);
                    }
                }
                if (itemId2 != 0) {
                    Item it2 = new Item();
                    for (int i = 0; i < itemQuantity2; i++) {
                        it2 = ItemData.itemDefault(itemId2);
                        nj.addItemBag(true, it2);
                    }
                }
                if (itemId3 != 0) {
                    Item it3 = new Item();
                    for (int i = 0; i < itemQuantity3; i++) {
                        it3 = ItemData.itemDefault(itemId3);
                        nj.addItemBag(true, it3);
                    }
                }
                if (itemId4 != 0) {
                    Item it4 = new Item();
                    for (int i = 0; i < itemQuantity4; i++) {
                        it4 = ItemData.itemDefault(itemId4);
                        nj.addItemBag(true, it4);
                    }
                }
                if (itemId5 != 0) {
                    Item it5 = new Item();
                    for (int i = 0; i < itemQuantity5; i++) {
                        it5 = ItemData.itemDefault(itemId5);
                        nj.addItemBag(true, it5);
                    }
                }
                if (messTB.length() > 0) {
                    this.server.manager.sendTB(this, "Thông báo", messTB);
                }
                SQLManager.executeUpdate("UPDATE `giftcode2` SET `luotnhap`='" + luotnhap + "' WHERE `id`=" + id + " LIMIT 1;");
                SQLManager.executeUpdate("UPDATE `giftcode2` SET `username`='" + userEntered + "' WHERE `id`=" + id + " LIMIT 1;");
            }
        });
    }

    public String nameUS;
    public void sendItem(User p, User p1, int id) {
        Item itemup = ItemData.itemDefault(id);
        p1.nj.addItemBag(false, itemup);
        p.sendYellowMessage("Gửi đồ thành công");
        p1.session.sendMessageLog("Bạn đã nhận được đồ từ admin");
        return;
    }

    public void chanXu(long xu) throws IOException {
        if (xu > this.nj.xu) {
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn không có đủ xu");
            return;
        }
        if (util.nextInt(0,1) == 0) {
            xu -= xu*15/100;
            this.nj.upxuMessage(xu);
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn vừa thắng " + xu + " xu");
        } else {
            this.nj.upxuMessage(-xu);
            this.nj.getPlace().chatNPC(this, (short)41, "Còn gì nữa đâu mà khóc với sầu");
        }
    }
    public void leXu(long xu) throws IOException {
        if (xu > this.nj.xu) {
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn không có đủ xu");
            return;
        }
        if (util.nextInt(0,1) == 1) {
            xu -= xu*15/100;
            this.nj.upxuMessage(xu);
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn vừa thắng " + xu + " xu");
        } else {
            this.nj.upxuMessage(-xu);
            this.nj.getPlace().chatNPC(this, (short)41, "Còn gì nữa đâu mà khóc với sầu");
        }
    }
    public void chanLuong(long luong) throws IOException {
        if (luong > this.luong) {
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn không có đủ lượng");
            return;
        }
        if (util.nextInt(0,1) == 0) {
            luong -= luong*15/100;
            this.upluongMessage(luong);
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn vừa thắng " + luong + " lượng");
        } else {
            this.upluongMessage(-luong);
            this.nj.getPlace().chatNPC(this, (short)41, "Còn gì nữa đâu mà khóc với sầu");
        }
    }
    public void leLuong(long luong) throws IOException {
        if (luong > this.luong) {
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn không có đủ lượng");
            return;
        }
        if (util.nextInt(0,1) == 1) {
            luong -= luong*15/100;
            this.upluongMessage(luong);
            this.nj.getPlace().chatNPC(this, (short)41, "Bạn vừa thắng " + luong + " lượng");
        } else {
            this.upluongMessage(-luong);
            this.nj.getPlace().chatNPC(this, (short)41, "Còn gì nữa đâu mà khóc với sầu");
        }
    }

    public void SendTree(final User session, final String url) throws IOException {
        final byte[] ab = GameScr.loadFile(url).toByteArray();
        final Message msg = new Message(117);
        msg.writer().write(ab);
        msg.writer().flush();
        session.sendMessage(msg);
        msg.cleanup();
    }
    
    public void pleaseInputParty(Message m) throws IOException {
        final String nameleader = m.reader().readUTF();
        m.cleanup();
        final Ninja n = PlayerManager.getInstance().getNinja(nameleader);
        if (this.nj.get().party == null) {
            m = new Message(23);
            m.writer().writeUTF(this.nj.name);
            m.writer().flush();
            n.p.sendMessage(m);
            m.cleanup();
        }
    }

    public void acceptPleaseParty(final Message m) throws IOException {
        final String nameplease = m.reader().readUTF();
        m.cleanup();
        if (this.nj.party == null) {
            return;
        }
        final Ninja n = PlayerManager.getInstance().getNinja(nameplease);
        if (n != null && n.party == null) {
            final Party party = this.nj.party;
            if (party.ninjas.size() > 5) {
                this.sendYellowMessage("Nhóm đã đủ thành viên");
                return;
            }
            party.addPartyAccept(n);
        } else {
            this.sendYellowMessage("Đối phương đã có nhóm rồi không thể gia nhập");
        }
    }
}
