package server;

import boardGame.Place;
import lombok.SneakyThrows;
import lombok.val;
import patch.*;
import patch.ItemShinwaManager.ItemShinwa;
import patch.clan.ClanTerritory;
import patch.clan.ClanTerritoryData;
import patch.interfaces.IBattle;
import patch.tournament.GeninTournament;
import patch.tournament.KageTournament;
import patch.tournament.Tournament;
import patch.tournament.Tournament.RegisterResult;
import patch.tournament.TournamentData;
import real.*;
import tasks.TaskHandle;
import tasks.TaskList;
import tasks.Text;
import threading.Manager;
import threading.Map;
import threading.Message;
import threading.Server;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static patch.Constants.TOC_TRUONG;
import static patch.ItemShinwaManager.*;
import static patch.TaskOrder.*;
import static patch.tournament.Tournament.*;
import static real.User.TypeTBLOption.*;

public class MenuController {

    public static final String MSG_HANH_TRANG = "Hành trang ko đủ chổ trống";

    public static final int MIN_YEN_NVHN = 100;
    public static final int MAX_YEN_NVHN = 200;
    LogHistory LogHistory = new LogHistory(this.getClass());

    Server server;

    public MenuController() {
        this.server = Server.getInstance();
    }

    public void sendMenu(final User p, final Message m) throws IOException {
        final byte npcId = m.reader().readByte();
        byte menuId = m.reader().readByte();
        final byte optionId = m.reader().readByte();


        val ninja = p.nj;

        if (TaskHandle.isTaskNPC(ninja, npcId) && Map.isNPCNear(ninja, npcId)) {
            // TODO SELECT MENU TASK
            menuId = (byte) (menuId - 1);
            if (ninja.getTaskIndex() == -1) {

                if (menuId == -1) {
                    TaskHandle.Task(ninja, (short) npcId);
                    return;
                }
            } else if (TaskHandle.isFinishTask(ninja)) {
                if (menuId == -1) {
                    TaskHandle.finishTask(ninja, (short) npcId);
                    return;
                }
            } else if (ninja.getTaskId() == 1) {
                if (menuId == -1) {
                    TaskHandle.doTask(ninja, (short) npcId, menuId, optionId);
                    return;
                }
            } else if (ninja.getTaskId() == 7) {
                if (menuId == -1) {
                    TaskHandle.doTask(ninja, (short) npcId, menuId, optionId);
                    return;
                }
            } else if (ninja.getTaskId() == 8 || ninja.getTaskId() == 0) {
                boolean npcTalking = TaskHandle.npcTalk(ninja, menuId, npcId);
                if (npcTalking) {
                    return;
                }

            } else if (ninja.getTaskId() == 13) {
                if (menuId == -1) {
                    if (ninja.getTaskIndex() == 1) {
                        // OOka
                        final Map map = Server.getMapById(56);
                        val place = map.getFreeArea();
                        val npc = Ninja.getNinja("Thầy Ookamesama");
                        npc.p = new User();
                        npc.p.nj = npc;
                        npc.isNpc = true;
                        npc.setTypepk(Constants.PK_DOSAT);
                        p.nj.enterSamePlace(place, npc);
                        return;
                    } else if (ninja.getTaskIndex() == 2) {
                        // Haru
                        final Map map = Server.getMapById(0);
                        val place = map.getFreeArea();
                        val npc = Ninja.getNinja("Thầy Kazeto");
                        if (npc == null) {
                            System.out.println("KO THẦY ĐỐ MÀY LÀM NÊN");
                            return;
                        }
                        npc.p = new User();
                        npc.isNpc = true;
                        npc.p.nj = npc;
                        npc.setTypepk(Constants.PK_DOSAT);
                        p.nj.enterSamePlace(place, npc);
                        return;
                    } else if (ninja.getTaskIndex() == 3) {
                        final Map map = Server.getMapById(73);

                        val npc = Ninja.getNinja("Cô Toyotomi");
                        if (npc == null) {
                            System.out.println("KO THẦY ĐỐ MÀY LÀM NÊN");
                            return;
                        }
                        npc.isNpc = true;
                        npc.p = new User();
                        npc.setTypepk(Constants.PK_DOSAT);
                        npc.p.nj = npc;
                        val place = map.getFreeArea();
                        p.nj.enterSamePlace(place, npc);
                        return;
                    }
                } else if (ninja.getTaskId() == 15 &&
                        ninja.getTaskIndex() >= 1) {
                    if (menuId == -1) {
                        // Nhiem vu giao thu
                        if (ninja.getTaskIndex() == 1 && npcId == 14) {
                            p.nj.removeItemBags(214, 1);
                        } else if (ninja.getTaskIndex() == 2 && npcId == 15) {
                            p.nj.removeItemBags(214, 1);
                        } else if (ninja.getTaskIndex() == 3 && npcId == 16) {
                            p.nj.removeItemBags(214, 1);
                        }
                    }

                }
            }
        }

        m.cleanup();
        Label_6355:
        {
            label:
            switch (p.typemenu) {
                case 0: {
                    if (menuId == 0) {
                        // Mua vu khi
                        p.openUI(2);
                        break;
                    }
                    switch (menuId) {
                        case 1:
                            if (optionId == 0) {
                                // Thanh lap gia toc
                                if (!p.nj.clan.clanName.isEmpty()) {
                                    p.nj.getPlace().chatNPC(p, (short) npcId, "Hiện tại con đã có gia tộc không thể thành lập thêm được nữa.");
                                    break label;
                                }
                                if (p.nj.luong < ClanManager.LUONG_CREATE_CLAN) {
                                    p.nj.getPlace().chatNPC(p, (short) npcId, "Để thành lập gia tộc con cần phải có đủ 20.000 lượng trong người.");
                                    break label;
                                }
                                this.sendWrite(p.nj, (short) 50, "Tên gia tộc");
                            } else if (optionId == 1) {
                                // Lanh địa gia tộc
                                if (p.getClanTerritoryData() == null) {
                                    if (p.nj.clan.typeclan == TOC_TRUONG) {

                                        if (p.nj.getAvailableBag() == 0) {
                                            p.sendYellowMessage("Hành trang không đủ để nhận chìa khoá");
                                            return;
                                        }
                                        val clan = ClanManager.getClanByName(p.nj.clan.clanName);
                                        if (clan.openDun <= 0) {
                                            p.sendYellowMessage("Số lần đi lãnh địa gia tộc đã hết vui lòng dùng thẻ bài hoặc đợi vào tuần");
                                            return;
                                        }

                                        val clanTerritory = new ClanTerritory(clan);
                                        Server.clanTerritoryManager.addClanTerritory(clanTerritory);
                                        p.setClanTerritoryData(new ClanTerritoryData(clanTerritory, p.nj));
                                        Server.clanTerritoryManager.addClanTerritoryData(p.getClanTerritoryData());

                                        clanTerritory.clanManager.openDun--;
                                        if (clanTerritory == null) {
                                            p.sendYellowMessage("Có lỗi xẩy ra");
                                            return;
                                        }
                                        val area = clanTerritory.getEntrance();
                                        if (area != null) {
                                            val item = ItemData.itemDefault(260);
                                            p.nj.addItemBag(false, item);
                                            if (p.getClanTerritoryData().getClanTerritory() != null) {

                                                if (p.getClanTerritoryData().getClanTerritory() != null) {
                                                    p.getClanTerritoryData().getClanTerritory().enterEntrance(p.nj);
                                                }

                                                clanTerritory.clanManager.informAll("Tộc trưởng đã mở lãnh địa gia tộc");
                                            } else {
                                                p.sendYellowMessage("Null sml");
                                            }
                                        } else {
                                            p.nj.getPlace().chatNPC(p, (short) npcId, "Hiện tại lãnh địa gia tộc không còn khu trống");
                                        }

                                    } else {
                                        p.sendYellowMessage("Chỉ những người ưu tú được tộc trưởng mời mới có thể vào lãnh địa gia tộc");
                                    }
                                } else {
                                    val data = p.getClanTerritoryData();
                                    if (data != null) {
                                        val teri = data.getClanTerritory();
                                        if (teri != null) teri.enterEntrance(p.nj);
                                    }
                                }

                            } else if (optionId == 2) {
                                int sum = 0;
                                for (Item item : p.nj.ItemBag) {
                                    if (item != null && item.id == 262) {
                                        sum += item.quantity;
                                    }
                                }
                                if (sum > 0) {
                                    p.nj.removeItemBags(262, sum);
                                    val item = ItemData.itemDefault(263);
                                    item.quantity = (int) (sum / 1.2);
                                    p.nj.addItemBag(true, item);
                                } else {
                                    p.sendYellowMessage("Không có xu gia tộc để đổi");
                                }

                            }
                            break label;
                        case 2:
                            if (menuId != 2) {
                                break label;
                            }
                            if (p.nj.isNhanban) {
                                p.session.sendMessageLog("Chức năng này không dành cho phân thân");
                                return;
                            }
                            if (optionId == 0) {
                                Service.evaluateCave(p.nj);
                                break label;
                            }
                            Cave cave = null;
                            if (p.nj.caveID != -1) {
                                if (Cave.caves.containsKey(p.nj.caveID)) {
                                    cave = Cave.caves.get(p.nj.caveID);
                                    p.nj.getPlace().leave(p);
                                    cave.map[0].area[0].EnterMap0(p.nj);
                                }
                            } else if (p.nj.party != null && p.nj.party.cave == null && p.nj.party.master != p.nj.id) {
                                p.session.sendMessageLog("Chỉ có nhóm trưởng mới được phép mở cửa hang động");
                                return;
                            }
                            if (cave == null) {
                                if (p.nj.nCave <= 0) {
                                    p.nj.getPlace().chatNPC(p, (short) npcId, "Số lần vào hang động của con hôm nay đã hết hãy quay lại vào ngày mai.");
                                    return;
                                }
                                if (optionId == 1) {
                                    if (p.nj.getLevel() < 30 || p.nj.getLevel() > 39) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }
                                    if (p.nj.party != null) {
                                        synchronized (p.nj.party.ninjas) {
                                            for (byte i = 0; i < p.nj.party.ninjas.size(); ++i) {
                                                if (p.nj.party.ninjas.get(i).getLevel() < 30 || p.nj.party.ninjas.get(i).getLevel() > 39) {
                                                    p.session.sendMessageLog("Thành viên trong nhóm trình độ không phù hợp");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    if (p.nj.party != null) {
                                        if (p.nj.party.cave == null) {
                                            cave = new Cave(3);
                                            p.nj.party.openCave(cave, p.nj.name);
                                        } else {
                                            cave = p.nj.party.cave;
                                        }
                                    } else {
                                        cave = new Cave(3);
                                    }
                                    p.nj.caveID = cave.caveID;
                                }
                                if (optionId == 2) {
                                    if (p.nj.getLevel() < 40 || p.nj.getLevel() > 49) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }
                                    if (p.nj.party != null) {
                                        synchronized (p.nj.party) {
                                            for (byte i = 0; i < p.nj.party.ninjas.size(); ++i) {
                                                if (p.nj.party.ninjas.get(i).getLevel() < 40 || p.nj.party.ninjas.get(i).getLevel() > 49) {
                                                    p.session.sendMessageLog("Thành viên trong nhóm trình độ không phù hợp");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    if (p.nj.party != null) {
                                        if (p.nj.party.cave == null) {
                                            cave = new Cave(4);
                                            p.nj.party.openCave(cave, p.nj.name);
                                        } else {
                                            cave = p.nj.party.cave;
                                        }
                                    } else {
                                        cave = new Cave(4);
                                    }
                                    p.nj.caveID = cave.caveID;
                                }
                                if (optionId == 3) {
                                    if (p.nj.getLevel() < 50 || p.nj.getLevel() > 59) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }
                                    if (p.nj.party != null) {
                                        synchronized (p.nj.party.ninjas) {
                                            for (byte i = 0; i < p.nj.party.ninjas.size(); ++i) {
                                                if (p.nj.party.ninjas.get(i).getLevel() < 50 || p.nj.party.ninjas.get(i).getLevel() > 59) {
                                                    p.session.sendMessageLog("Thành viên trong nhóm trình độ không phù hợp");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    if (p.nj.party != null) {
                                        if (p.nj.party.cave == null) {
                                            cave = new Cave(5);
                                            p.nj.party.openCave(cave, p.nj.name);
                                        } else {
                                            cave = p.nj.party.cave;
                                        }
                                    } else {
                                        cave = new Cave(5);
                                    }
                                    p.nj.caveID = cave.caveID;
                                }
                                if (optionId == 4) {
                                    if (p.nj.getLevel() < 60 || p.nj.getLevel() > 69) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }
                                    if (p.nj.party != null && p.nj.party.ninjas.size() > 1) {
                                        p.session.sendMessageLog("Hoạt động lần này chỉ được phép một mình");
                                        return;
                                    }
                                    cave = new Cave(6);
                                    p.nj.caveID = cave.caveID;
                                }
                                if (optionId == 5) {
                                    if (p.nj.getLevel() < 70 || p.nj.getLevel() > 89) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }
                                    if (p.nj.party != null) {
                                        synchronized (p.nj.party.ninjas) {
                                            for (byte i = 0; i < p.nj.party.ninjas.size(); ++i) {
                                                if (p.nj.party.ninjas.get(i).getLevel() < 70) {
                                                    p.session.sendMessageLog("Thành viên trong nhóm trình độ không phù hợp");
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    if (p.nj.party != null) {
                                        if (p.nj.party.cave == null) {
                                            cave = new Cave(7);
                                            p.nj.party.openCave(cave, p.nj.name);
                                        } else {
                                            cave = p.nj.party.cave;
                                        }
                                    } else {
                                        cave = new Cave(7);
                                    }
                                    p.nj.caveID = cave.caveID;
                                }
//                                p.session.sendMessageLog("Bảo trì");
                                if (optionId == 6) {
                                    if (p.nj.getLevel() < 90 || p.nj.getLevel() > 130) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }

                                    if (p.nj.party != null && p.nj.party.getKey() != null &&
                                            p.nj.party.getKey().get().getLevel() >= 90) {
                                        synchronized (p.nj.party.ninjas) {
                                            for (byte i = 0; i < p.nj.party.ninjas.size(); ++i) {
                                                if (p.nj.party.ninjas.get(i).getLevel() < 90 || p.nj.party.ninjas.get(i).getLevel() > 131) {
                                                    p.session.sendMessageLog("Thành viên trong nhóm trình độ không phù hợp");
                                                    return;
                                                }
                                            }
                                        }
                                    }

                                    if (p.nj.party != null) {
                                        if (p.nj.party.cave == null) {
                                            cave = new Cave(9);
                                            p.nj.party.openCave(cave, p.nj.name);
                                        } else {
                                            cave = p.nj.party.cave;
                                        }
                                    } else {
                                        cave = new Cave(9);
                                    }
                                    p.nj.caveID = cave.caveID;
                                }
                                // hang động 130
                                if (optionId == 7) {
                                    if (p.nj.getLevel() < 130 || p.nj.getLevel() > 150) {
                                        p.session.sendMessageLog("Trình độ không phù hợp");
                                        return;
                                    }

                                    if (p.nj.party != null && p.nj.party.getKey() != null &&
                                            p.nj.party.getKey().get().getLevel() >= 130) {
                                        synchronized (p.nj.party.ninjas) {
                                            for (byte i = 0; i < p.nj.party.ninjas.size(); ++i) {
                                                if (p.nj.party.ninjas.get(i).getLevel() < 130 || p.nj.party.ninjas.get(i).getLevel() > 151) {
                                                    p.session.sendMessageLog("Thành viên trong nhóm trình độ không phù hợp");
                                                    return;
                                                }
                                            }
                                        }
                                    }

                                    if (p.nj.party != null) {
                                        if (p.nj.party.cave == null) {
                                            cave = new Cave(10);
                                            p.nj.party.openCave(cave, p.nj.name);
                                        } else {
                                            cave = p.nj.party.cave;
                                        }
                                    } else {
                                        cave = new Cave(10);
                                    }
                                    p.nj.caveID = cave.caveID;
                                }
                                if (cave != null) {
                                    final Ninja c = p.nj;
                                    --c.nCave;
                                    p.nj.pointCave = 0;
                                    p.nj.getPlace().leave(p);
                                    cave.map[0].area[0].EnterMap0(p.nj);
                                }
                            }
                            p.setPointPB(p.nj.pointCave);
                            break label;
                        case 3: {
                            if (optionId == 0) {
                                // Thach dau loi dai
                                this.sendWrite(p.nj, (short) 2, "Nhập tên đối thủ của ngươi vào đây");
                                break;
                            } else if (optionId == 1) {
                                // Xem thi dau
                                Service.sendBattleList(p);
                            }

                        }

                    }
                }
                case 1: {
                    if (menuId != 0) {
                        break;
                    }
                    if (optionId == 0) {
                        p.openUI(21 - p.nj.gender);
                        break;
                    }
                    if (optionId == 1) {
                        p.openUI(23 - p.nj.gender);
                        break;
                    }
                    if (optionId == 2) {
                        p.openUI(25 - p.nj.gender);
                        break;
                    }
                    if (optionId == 3) {
                        p.openUI(27 - p.nj.gender);
                        break;
                    }
                    if (optionId == 4) {
                        p.openUI(29 - p.nj.gender);
                        break;
                    }
                    break;
                }
                case 2: {
                    if (menuId == 0) {
                        if (optionId == 0) {
                            p.openUI(16);
                            break;
                        } else if (optionId == 1) {
                            p.openUI(17);
                            break;
                        } else if (optionId == 2) {
                            p.openUI(18);
                            break;
                        } else if (optionId == 3) {
                            p.openUI(19);
                            break;
                        }
                    } else if (menuId == 1) {
                        ItemData data;
                        if (optionId == 0) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            if (p.nj.getLevel() < 50) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Cấp độ của con cần đạt 50 để nhận nhiệm vụ này");
                                return;
                            }

                            if (p.nj.countTaskDanhVong < 1) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Số lần nhận nhiệm vụ của con hôm nay đã hết");
                                return;
                            }

                            if (p.nj.isTaskDanhVong == 1) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Trước đó con đã nhận nhiệm vụ rồi, hãy hoàn thành đã nha");
                                return;
                            }
                            p.nj.nhiemvuDV = true;
                            int type = DanhVongData.randomNVDV();
                            p.nj.taskDanhVong[0] = type;
                            p.nj.taskDanhVong[1] = 0;
                            p.nj.taskDanhVong[2] = DanhVongData.targetTask(type);
                            p.nj.isTaskDanhVong = 1;
                            p.nj.countTaskDanhVong--;
                            if (p.nj.isTaskDanhVong == 1) {
                                String nv = "NHIỆM VỤ LẦN NÀY: \n"
                                        + String.format(DanhVongData.nameNV[p.nj.taskDanhVong[0]],
                                        p.nj.taskDanhVong[1],
                                        p.nj.taskDanhVong[2])
                                        + "\n\n- Số lần nhận nhiệm vụ còn lại là: " + p.nj.countTaskDanhVong;
                                server.manager.sendTB(p, "Nhiệm vụ", nv);
                            }
                            break;
                        } else if (optionId == 1) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            if (p.nj.isTaskDanhVong == 0) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Con chưa nhận nhiệm vụ nào cả!");
                                return;
                            }

                            if (p.nj.taskDanhVong[1] < p.nj.taskDanhVong[2]) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Con chưa hoàn thành nhiệm vụ ta giao!");
                                return;
                            }

                            if (p.nj.getAvailableBag() < 2) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không đủ chỗ trống để nhận thưởng");
                                return;
                            }

                            int point = 5;
                            if (p.nj.taskDanhVong[0] == 9) {
                                point = 10;
                            }

                            p.nj.isTaskDanhVong = 0;
                            p.nj.taskDanhVong = new int[]{-1, -1, -1, 0, p.nj.countTaskDanhVong};
                            Item item = ItemData.itemDefault(695);
                            item.quantity = 1;
                            item.setLock(true);
                            if (p.nj.pointUydanh < 5000) {
                                ++p.nj.pointUydanh;
                            }

                            p.nj.addItemBag(true, item);
                            int type = util.nextInt(10);

                            if (p.nj.avgPointDanhVong(p.nj.getPointDanhVong(type))) {
                                for (int i = 0; i < 10; i++) {
                                    type = i;
                                    if (!p.nj.avgPointDanhVong(p.nj.getPointDanhVong(type))) {
                                        break;
                                    }
                                }
                            }
                            p.nj.plusPointDanhVong(type, point);

                            if (p.nj.countTaskDanhVong % 2 == 0) {
                                Item itemUp = ItemData.itemDefault(p.nj.gender == 1 ? -1 : -1, true);
                                itemUp.setLock(true);
                                itemUp.isExpires = false;
                                itemUp.expires = -1L;
                                itemUp.quantity = util.nextInt(1, 2);
                                p.nj.addItemBag(true, itemUp);
                            } else {
                                Item itemUp = ItemData.itemDefault(p.nj.gender == 1 ? -1 : -1, true);
                                itemUp.setLock(true);
                                itemUp.isExpires = false;
                                itemUp.expires = -1L;
                                itemUp.quantity = util.nextInt(1, 2);
                                p.nj.addItemBag(true, itemUp);
                            }

                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con hãy nhận lấy phần thưởng của mình.");
                            break;
                        } else if (optionId == 2) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            if (p.nj.isTaskDanhVong == 0) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Con chưa nhận nhiệm vụ nào cả!");
                                return;
                            }

                            Service.startYesNoDlg(p, (byte) 2, "Con có chắc chắn muốn huỷ nhiệm vụ lần này không?");
                            break;
                        } else if (optionId == 3) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            if (p.nj.checkPointDanhVong(1)) {
                                if (p.nj.getAvailableBag() < 1) {
                                    p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không đủ chỗ trống để nhận thưởng");
                                    return;
                                }

                                Item item = ItemData.itemDefault(685, true);
                                item.quantity = 1;
                                item.setUpgrade(1);
                                item.setLock(true);
                                Option op = new Option(6, 1000);
                                item.option.add(op);
                                op = new Option(87, 500);
                                item.option.add(op);
                                p.nj.addItemBag(false, item);
                            } else {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Con chưa đủ điểm để nhận mắt");
                            }

                            break;
                        } else if (optionId == 4) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            if (p.nj.ItemBody[14] == null) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Hãy đeo mắt vào người trước rồi nâng cấp nhé.");
                                return;
                            }

                            if (p.nj.ItemBody[14] == null) {
                                return;
                            }

                            if (p.nj.ItemBody[14].getUpgrade() >= 10) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Mắt của con đã đạt cấp tối đa");
                                return;
                            }

                            if (!p.nj.checkPointDanhVong(p.nj.ItemBody[14].getUpgrade())) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Con chưa đủ điểm danh vọng để thực hiện nâng cấp");
                                return;
                            }

                            data = ItemData.ItemDataId(p.nj.ItemBody[14].id);
                            Service.startYesNoDlg(p, (byte) 0, "Bạn có muốn nâng cấp " + data.name + " với " + GameScr.coinUpMat[p.nj.ItemBody[14].getUpgrade()] + " yên hoặc xu với tỷ lệ thành công là " + GameScr.percentUpMat[p.nj.ItemBody[14].getUpgrade()] + "% không?");
                            break;
                        } else if (optionId == 5) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            if (p.nj.ItemBody[14] == null) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Hãy đeo mắt vào người trước rồi nâng cấp nhé.");
                                return;
                            }

                            if (p.nj.ItemBody[14].getUpgrade() >= 10) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Mắt của con đã đạt cấp tối đa");
                                return;
                            }

                            if (!p.nj.checkPointDanhVong(p.nj.ItemBody[14].getUpgrade())) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Con chưa đủ điểm danh vọng để thực hiện nâng cấp");
                                return;
                            }

                            data = ItemData.ItemDataId(p.nj.ItemBody[14].id);
                            Service.startYesNoDlg(p, (byte) 1, "Bạn có muốn nâng cấp " + data.name + " với " + GameScr.coinUpMat[p.nj.ItemBody[14].getUpgrade()] + " yên hoặc xu và " + GameScr.goldUpMat[p.nj.ItemBody[14].getUpgrade()] + " lượng với tỷ lệ thành công là " + GameScr.percentUpMat[p.nj.ItemBody[14].getUpgrade()] * 2 + "% không?");
                            break;
                        } else if (optionId == 6) {
                            if (p.nj.isNhanban) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Phân thân không thể sử dụng chức năng này.");
                                return;
                            }

                            String nv = "- Hoàn thành nhiệm vụ. Hãy gặp Ameji để trả nhiệm vụ.\n- Hôm nay có thể nhận thêm " + p.nj.countTaskDanhVong + " nhiệm vụ trong ngày.\n- Hôm nay có thể sử dụng thêm " + p.nj.useDanhVongPhu + " Danh Vọng Phù để nhận thêm 5 lần làm nhiệm vụ.\n- Hoàn thành nhiệm vụ sẽ nhận 1 viên đá danh vọng cấp 1.\n- Khi đủ mốc 100 điểm mỗi loại có thể nhận mắt và nâng cấp mắt.";
                            if (p.nj.isTaskDanhVong == 1) {
                                nv = "NHIỆM VỤ LẦN NÀY: \n" + String.format(DanhVongData.nameNV[p.nj.taskDanhVong[0]], p.nj.taskDanhVong[1], p.nj.taskDanhVong[2]) + "\n\n" + nv;
                            }

                            server.manager.sendTB(p, "Nhiệm vụ", nv);
                            break;
                        }
                    } else if (menuId == 2) {
                        int value = util.nextInt(3);
                        if (value == 0) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con chọn loại trang sức gì nào?");
                        }
                        if (value == 1) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Trang sức không chỉ để ngắm, nó còn tăng sức mạnh của con");
                        }
                        if (value == 2) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con cần mua ngọc bội, nhẫn, dây chuyền, bùa họ thân à?");
                        }
                        break label;
                    }
                }
//                case 2: {
//                    if (menuId == 0) {
//                        if (optionId == 0) {
//                            p.openUI(16);
//                            break;
//                        } else if (optionId == 1) {
//                            p.openUI(17);
//                            break;
//                        } else if (optionId == 2) {
//                            p.openUI(18);
//                            break;
//                        } else if (optionId == 3) {
//                            p.openUI(19);
//                            break;
//                        }
//                    } else if (menuId == 1) {
//                        if (optionId == 4) {
//                            // Nang mat thuong
//                            final val item = p.nj.get().ItemBody[14];
//                            if (item != null && item.getUpgrade() != 0) {
//                                nangMat(p, item, false);
//                            } else {
//                                p.sendYellowMessage("Phải đeo mắt mới có thể nâng cấp");
//                            }
//
//                        } else if (optionId == 5) {
//                            // Nang mắt vip
//                            final val item = p.nj.get().ItemBody[14];
//                            if (item != null && item.getUpgrade() != 0) {
//                                nangMat(p, item, true);
//                            } else {
//                                p.sendYellowMessage("Phải đeo mắt mới có thể nâng cấp");
//                            }
//                        } else if (optionId == 6) {
//                            final List<int[]> data = MenuController.nangCapMat.keySet().stream()
//                                    .map(s -> nangCapMat.get(s)).collect(Collectors.toList());
//
//                            String s = "Sử dụng vật phẩm sự kiện để có thể nhận mắt 1\n";
//                            for (int i = 0, dataSize = data.size(); i < dataSize; i++) {
//                                int[] datum = data.get(i);
//                                s += "-Nâng cấp mắt " + (i + 2) + " dùng " + datum[0] + " viên đá danh vọng cấp " + (i + 2) + " nâng thường " + datum[1] + " xu xác suất " + datum[2] + "%, VIP " + datum[1] + " xu " + datum[3] + " lượng xác suất " + datum[4] + "% \n\n";
//                            }
//                            server.manager.sendTB(p, "Hướng dẫn", s);
//                        }
//                    }
//                    break;
//                }
                case 3: {
                    if (menuId == 0) {
                        p.openUI(7);
                        break;
                    }
                    if (menuId == 1) {
                        p.openUI(6);
                        break;
                    }
                    break;
                }
                case 4: {
                    switch (menuId) {
                        case 0: {
                            p.openUI(9);
                            break;
                        }
                        case 1: {
                            p.openUI(8);
                            break;
                        }
                        case 2: {
                            int value = util.nextInt(1);
                            if (value == 0) {
                                p.nj.getPlace().chatNPC(p, (short) 4, "Ăn Xong đảm bảo người sẽ quay lại lần sau");
                            }
                            break;
                        }
                        case 3: {
                            switch (optionId) {
                                case 0: {
                                    // Đăng kí thien dia bang
                                    if (p.nj.get().getLevel() < 50) {
                                        p.nj.getPlace().chatNPC(p, (short) 4, "Yêu cầu trình độ cấp 50");
                                        return;
                                    }
                                    RegisterResult result = null;
                                    if (p.nj.get().getLevel() <= 80) {
                                        result = GeninTournament.gi().register(p);

                                    } else if (p.nj.get().getLevel() > 80 && p.nj.get().getLevel() <= 130) {
                                        result = KageTournament.gi().register(p);
                                    }

                                    if (result != null) {
                                        if (result == RegisterResult.SUCCESS) {
                                            p.nj.getPlace().chatNPC(p, (short) 4, "Bạn đã đăng kí thành công");
                                        } else if (result == RegisterResult.ALREADY_REGISTER) {
                                            p.nj.getPlace().chatNPC(p, (short) 4, "Bạn đã đăng kí thành công rồi");
                                        } else if (result == RegisterResult.LOSE) {
                                            p.nj.getPlace().chatNPC(p, (short) 4, "Bạn đã thua không thể đăng kí được");
                                        }
                                    } else {

                                    }
                                    break;
                                }
                                case 1: {
                                    //Chinh phuc thien dia bang
                                    try {
                                        final List<TournamentData> tournaments = getTypeTournament(p.nj.getLevel()).getChallenges(p);
                                        Service.sendChallenges(tournaments, p);
                                    } catch (Exception e) {

                                    }

                                    break;
                                }
                                case 2: {
                                    //Thien bang
                                    sendThongBaoTDB(p, KageTournament.gi(), "Thiên bảng\n");
                                    break;
                                }
                                case 3: {
                                    // Dia bang
                                    sendThongBaoTDB(p, GeninTournament.gi(), "Địa bảng\n");
                                    break;
                                }
                            }
                            break;
                        }
                        
                    }
                    break;
                }
                case 5: {
                    switch (menuId) {
                        case 0: {
                            p.typemenu = 699;
                            doMenuArray(p, new String[]{"Mở rương", "Mở bộ sưu tập", "Cải trang", "Tháo cải trang"});
                            break;
                        }
                        case 1: {
                            if (p.nj.getPlace().map.template.id == 1 || p.nj.getPlace().map.template.id == 27 || p.nj.getPlace().map.template.id == 72 || p.nj.getPlace().map.template.id == 10 || p.nj.getPlace().map.template.id == 17 || p.nj.getPlace().map.template.id == 22 || p.nj.getPlace().map.template.id == 32 || p.nj.getPlace().map.template.id == 38 || p.nj.getPlace().map.template.id == 43 || p.nj.getPlace().map.template.id == 48) {
                                p.nj.mapLTD = p.nj.getPlace().map.id;
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Lưu tọa độ thành công, khi kiệt sức con sẽ được khiêng về đây");
                            }
                            break;
                        }
                        case 2: {
                            if (optionId != 0) {
                                break;
                            }
                            // TODO Bo gioi up lv vdmq phan than
//                            if (p.nj.isNhanban) {
//                                p.session.sendMessageLog("Chức năng này không dành cho phân thân");
//                                return;
//                            }

//                            if (p.nj.getEffId(34) == null) {
//                                p.nj.getPlace().chatNPC(p, (short) 5, "Phải dùng thí luyện thiếp mới có thể vào được");
//                                return;
//                            }
                            if (p.nj.getLevel() < 60) {
                                p.session.sendMessageLog("Chức năng yêu cầu trình độ 60");
                                return;
                            }
                            final Manager manager = this.server.manager;
                            final Map ma = Manager.getMapid(139);
                            for (final Place area : ma.area) {
                                if (area.getNumplayers() < ma.template.maxplayers) {
                                    p.nj.getPlace().leave(p);
                                    area.EnterMap0(p.nj);
                                    return;
                                }
                            }
                            break;
                        }
                        case 3:{

                            break;
                        }
                        case 4: {
                            p.nj.ItemGiaHan.clear();
                            for (int i = 0; i < p.nj.ItemBag.length; i++) {
                                Item imb = p.nj.ItemBag[i];
                                if (imb != null && imb.isExpires == true && imb.expires > 0) {
                                    p.nj.ItemGiaHan.add(imb);
                                }
                            }
                            Service.openMenuGiaHan(p);
                            break;
                        }
                        case 5: {
                            p.typemenu = 700;
                            doMenuArray(p, new String[]{"Quà 2h", "Quà cấp 30"});
                            break;
                        }
                    }
                    break;
                }
                case 699: {
                    switch (menuId) {
                        case 0: {
                            Service.openMenuBox(p);
                            break;
                        }
                        case 1: {
                            Service.openMenuBST(p);
                            break;
                        }
                        case 2: {
                            Service.openMenuCaiTrang(p);
                            break;
                        }
                        case 3: {
                            //Tháo cải trang
                            if (p.nj.ItemBodyHide[0] != null) {
                                Service.thaoCaiTrang(p);
                            } else {
                                break;
                            }
                        }
                    }
                    break;
                }
                case 6: {
                    switch (menuId) {
                        case 0: {
                            if (optionId == 0) {
                                p.openUI(10);
                                break;
                            }
                            if (optionId == 1) {
                                p.openUI(31);
                                break;
                            }
                            break;
                        }
                        case 1: {
                            if (optionId == 0) {
                                p.openUI(12);
                                break;
                            }
                            if (optionId == 1) {
                                p.openUI(11);
                                break;
                            }
                            break;
                        }
                        case 2: {
                            p.openUI(13);
                            break;
                        }
                        case 3: {
                            p.openUI(33);
                            break;
                        }
                        case 4: {
                            // Luyen ngoc
                            p.openUI(46);
                            break;
                        }
                        case 5: {
                            // Kham ngoc
                            p.openUI(47);
                            break;
                        }
                        case 6: {
                            // Got ngoc
                            p.openUI(49);
                            break;
                        }
                        case 7: {
                            // Thao ngoc
                            p.openUI(50);
                            break;
                        }
                        case 8: {
                            int value = util.nextInt(3);
                            if (value == 0) {
                                p.nj.getPlace().chatNPC(p, (short) 6, "Người muốn cải tiến trang bị?");
                            }
                            if (value == 1) {
                                p.nj.getPlace().chatNPC(p, (short) 6, "Nâng cấp trang bị: Uy tính, giá cả phải chăng.");
                            }
                            if (value == 2) {
                                p.nj.getPlace().chatNPC(p, (short) 6, "Đảm bảo sau khi nâng cấp đồ của ngươi sẽ tốt hơn hẳn");
                            }
                            break;
                        }
                    }
                    break;
                }
                case 7: {
                    if (menuId == 0) {
                        break;
                    }
                    if (menuId > 0 && menuId <= Map.arrLang.length) {
                        final Map ma = Manager.getMapid(Map.arrLang[menuId - 1]);
                        for (final Place area : ma.area) {
                            if (area.getNumplayers() < ma.template.maxplayers) {
                                p.nj.getPlace().leave(p);
                                area.EnterMap0(p.nj);
                                return;
                            }
                        }
                        break;
                    }
                    break;
                }
                case 8: {
                    if (menuId >= 0 && menuId < Map.arrTruong.length) {
                        final Map ma = Manager.getMapid(Map.arrTruong[menuId]);
                        for (final Place area : ma.area) {
                            if (area.getNumplayers() < ma.template.maxplayers) {
                                p.nj.getPlace().leave(p);
                                area.EnterMap0(p.nj);
                                return;
                            }
                        }
                        break;
                    }
                    break;
                }
                case 9: {
                    if (menuId == 0) {
                        if (optionId == 0) {
                            this.server.manager.sendTB(p, "Top đại gia yên", BXHManager.getStringBXH(0));
                        } else if (optionId == 1) {
                            this.server.manager.sendTB(p, "Top cao thủ", BXHManager.getStringBXH(1));
                        } else if (optionId == 2) {
                            this.server.manager.sendTB(p, "Top gia tộc", BXHManager.getStringBXH(2));
                        } else if (optionId == 3) {
                            this.server.manager.sendTB(p, "Top hang động", BXHManager.getStringBXH(3));
                        }
                    }
                    if (menuId == 1) {
                        if (p.nj.get().getLevel() < 10) {
                            return;
                        }
                        if (p.nj.get().nclass > 0) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con đã vào lớp từ trước rồi mà");
                            break;
                        }
                        if (p.nj.get().ItemBody[1] != null) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con cần tháo vũ khí ra để đến đây nhập học nhé");
                            break;
                        }
                        if (p.nj.getAvailableBag() < 3) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang phải có đủ 2 ô để nhận đồ con nhé");
                            break;
                        }
//                        p.nj.addItemBag(false, ItemData.itemDefault(420));
                        if (optionId == 0) {
                            p.Admission((byte) 1);
                        } else if (optionId == 1) {
                            p.Admission((byte) 2);
                        }
                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hãy chăm chỉ tập luyện để lên cấp con nhé");
                        break;
                    } else {
                        if (menuId != 2) {
                            break;
                        }
                        if (p.nj.get().nclass != 1 && p.nj.get().nclass != 2) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con không phải học sinh trường này nên không thể tẩy điểm ở đây");
                            break;
                        }
                        if (optionId == 0) {
                            if (p.nj.taytn < 1) {
                                p.nj.getPlace().chatNPC(p, (short)npcId, "Con không có đủ số lần tẩy tiềm năng");
                                break;
                            }
                            p.restPpoint(p.nj.get());
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã giúp con tẩy điểm tiềm năng, hãy sử dụng tốt điểm tiềm năng nhé");
                            break;
                        }
                        if (optionId == 1) {
                            if (p.nj.taykn < 1) {
                                p.nj.getPlace().chatNPC(p, (short)npcId, "Con không có đủ số lần tẩy kĩ năng");
                                break;
                            }
                            p.restSpoint();
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã giúp con tẩy điểm kĩ năng, hãy sử dụng tốt điểm kĩ năng nhé");
                            break;
                        }

                        break;
                    }
                }
                case 10: {
                    if (menuId == 0) {
                        if (optionId == 0) {
                            this.server.manager.sendTB(p, "Top đại gia yên", BXHManager.getStringBXH(0));
                        } else if (optionId == 1) {
                            this.server.manager.sendTB(p, "Top cao thủ", BXHManager.getStringBXH(1));
                        } else if (optionId == 2) {
                            this.server.manager.sendTB(p, "Top gia tộc", BXHManager.getStringBXH(2));
                        } else if (optionId == 3) {
                            this.server.manager.sendTB(p, "Top hang động", BXHManager.getStringBXH(3));
                        }
                    }
                    if (menuId == 1) {
                        if (p.nj.get().getLevel() < 10) {
                            return;
                        }
                        if (p.nj.get().nclass > 0) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con đã vào lớp từ trước rồi mà");
                            break;
                        }
                        if (p.nj.get().ItemBody[1] != null) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con cần tháo vũ khí ra để đến đây nhập học nhé");
                            break;
                        }
                        if (p.nj.getAvailableBag() < 3) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang phải có đủ 2 ô để nhận đồ con nhé");
                            break;
                        }
//                        p.nj.addItemBag(false, ItemData.itemDefault(421));
                        if (optionId == 0) {
                            p.Admission((byte) 3);
                        } else if (optionId == 1) {
                            p.Admission((byte) 4);
                        }
                        p.nj.getPlace().chatNPC(p, (short) 9, "Hãy chăm chỉ tập luyện để lên cấp con nhé");
                        break;
                    } else {
                        if (menuId != 2) {
                            break;
                        }
                        if (p.nj.get().nclass != 3 && p.nj.get().nclass != 4) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con không phải học sinh trường này nên không thể tẩy điểm ở đây");
                            break;
                        }
                        if (optionId == 0) {
                            if (p.nj.taytn < 1) {
                                p.nj.getPlace().chatNPC(p, (short)npcId, "Con không có đủ số lần tẩy tiềm năng");
                                break;
                            }
                            p.restPpoint(p.nj.get());
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã giúp con tẩy điểm tiềm năng, hãy sử dụng tốt điểm tiềm năng nhé");
                            break;
                        }
                        if (optionId == 1) {
                            if (p.nj.taykn < 1) {
                                p.nj.getPlace().chatNPC(p, (short)npcId, "Con không có đủ số lần tẩy kĩ năng");
                                break;
                            }
                            p.restSpoint();
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã giúp con tẩy điểm kĩ năng, hãy sử dụng tốt điểm kĩ năng nhé");
                            break;
                        }
                        break;
                    }
                }
                case 11: {
                    if (menuId == 0) {
                        if (optionId == 0) {
                            this.server.manager.sendTB(p, "Top đại gia yên", BXHManager.getStringBXH(0));
                        } else if (optionId == 1) {
                            this.server.manager.sendTB(p, "Top cao thủ", BXHManager.getStringBXH(1));
                        } else if (optionId == 2) {
                            this.server.manager.sendTB(p, "Top gia tộc", BXHManager.getStringBXH(2));
                        } else if (optionId == 3) {
                            this.server.manager.sendTB(p, "Top hang động", BXHManager.getStringBXH(3));
                        }
                    }
                    if (menuId == 1) {
                        if (p.nj.get().getLevel() < 10) {
                            return;
                        }
                        if (p.nj.get().nclass > 0) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con đã vào lớp từ trước rồi mà");
                            break;
                        }
                        if (p.nj.get().ItemBody[1] != null) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con cần tháo vũ khí ra để đến đây nhập học nhé");
                            break;
                        }
                        if (p.nj.getAvailableBag() < 3) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang phải có đủ 2 ô để nhận đồ con nhé");
                            break;
                        }
//                        p.nj.addItemBag(false, ItemData.itemDefault(422));
                        if (optionId == 0) {
                            p.Admission((byte) 5);
                        } else if (optionId == 1) {
                            p.Admission((byte) 6);
                        }
                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hãy chăm chỉ tập luyện để lên cấp con nhé");
                        break;
                    } else {
                        if (menuId != 2) {
                            break;
                        }
                        if (p.nj.get().nclass != 5 && p.nj.get().nclass != 6) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Con không phải học sinh trường này nên không thể tẩy điểm ở đây");
                            break;
                        }
                        if (optionId == 0) {
                            if (p.nj.taytn < 1) {
                                p.nj.getPlace().chatNPC(p, (short)npcId, "Con không có đủ số lần tẩy tiềm năng");
                                break;
                            }
                            p.restPpoint(p.nj.get());
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã giúp con tẩy điểm tiềm năng, hãy sử dụng tốt điểm tiềm năng nhé");
                            break;
                        }
                        if (optionId == 1) {
                            if (p.nj.taykn < 1) {
                                p.nj.getPlace().chatNPC(p, (short)npcId, "Con không có đủ số lần tẩy kĩ năng");
                                break;
                            }
                            p.restSpoint();
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã giúp con tẩy điểm kĩ năng, hãy sử dụng tốt điểm kĩ năng nhé");
                            break;
                        }
                        break;
                    }
                }
                // tajima
                case 12: {
                    if (menuId == 0) {
                        break;
                    } else if (menuId == 2) {
                        p.nj.clearTask();
                        p.nj.getPlace().chatNPC(p, (short) npcId, "Ta đã huỷ hết nhiệm vụ và vật phẩm nhiệm vụ của con lần sau làm nhiệm vụ tốt hơn nhé");
                        Service.getTask(p.nj);
                        break;
                    } else if (menuId == 3) {
                        if (p.nj.timeRemoveClone > System.currentTimeMillis()) {
                            p.toNhanBan();
                            break;
                        }
                        break;
                    } else if (menuId == 4) {
                        if (!p.nj.clone.isDie && p.nj.timeRemoveClone > System.currentTimeMillis() && p.nj.isNhanban) {
                            p.exitNhanBan(false);
                            p.nj.clone.open(p.nj.timeRemoveClone, p.nj.getPramSkill(71));
                            break;
                        }
                        break;
                    } else if (menuId == 5) {
                        if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(500000L);
                            Item item = ItemData.itemDefault(797);//yy top1
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            item  = ItemData.itemDefault(830);//mn ho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                            break;
                    } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(300000L);
                            Item item = ItemData.itemDefault(797);//yy top2
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            item  = ItemData.itemDefault(830);//mn ho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                            break;
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(200000L);
                            Item item = ItemData.itemDefault(797);//yy top3
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            item  = ItemData.itemDefault(830);//mn ho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(10000L);
                            Item item = ItemData.itemDefault(797);//yy top4
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(830);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            item  = ItemData.itemDefault(830);//mn ho
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 2592000000L;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                            
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(80000L);
                            Item item = ItemData.itemDefault(797);//yy top5
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(830);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            item  = ItemData.itemDefault(830);//mn ho
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 2592000000L;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(70000L);
                            Item item = ItemData.itemDefault(797);//yy top6
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 7776000000L;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            p.nj.quatop = true;
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(60000L);
                            Item item = ItemData.itemDefault(797);//yy top7
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 7776000000L;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);
                            p.nj.quatop = true;
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(0L);
                            Item item = ItemData.itemDefault(797);//bachho top8
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 7776000000L;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                        } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(40000L);
                            Item item = ItemData.itemDefault(797);//bachho top9
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 5184000000L;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                            } else if (p.nj.name.equals("") && p.nj.quatop == false) {
                            p.upluongMessage(30000L);
                            Item item = ItemData.itemDefault(797);//bachho top10
                            item.setLock(false);
                            item.isExpires = true;
                            item.expires = System.currentTimeMillis() + 2592000000L;
                            p.nj.addItemBag(false, item);
                            
                            item  = ItemData.itemDefault(831);//bachho
                            item.setLock(false);
                            item.isExpires = false;
                            item.expires = -1;
                            p.nj.addItemBag(false, item);

                            p.nj.quatop = true;
                        } else {
                            p.session.sendMessageLog("Con Không Có Trong Danh Sách Nhận Quà  !!");
                        }
                        break;
                    }
                    p.nj.getPlace().chatNPC(p, (short) npcId, "Con đang thực hiện nhiệm vụ kiên trì diệt ác, hãy chọn Menu/Nhiệm vụ để biết mình đang làm đến đâu");
                    break;
                }

                case 14:
                case 15:
                case 16: {
                    boolean hasItem = false;
                    for (Item item : p.nj.ItemBag) {
                        if (item != null && item.id == 214) {
                            hasItem = true;
                            break;
                        }
                    }
                    if (hasItem) {
                        p.nj.removeItemBags(214, 1);
                        p.nj.getPlace().chatNPC(p, npcId, "Ta rất vui vì cô béo còn quan tâm đến ta.");
                        p.nj.upMainTask();
                    } else {
                        if (p.nj.getTaskId() == 20 && p.nj.getTaskIndex() == 1 && npcId == 15) {
                            p.nj.getPlace().leave(p);
                            final Map map = Server.getMapById(74);
                            val place = map.getFreeArea();
                            synchronized (place) {
                                p.expiredTime = System.currentTimeMillis() + 600000L;
                            }
                            Service.batDauTinhGio(p, 600);
                            place.refreshMobs();
                            place.EnterMap0(p.nj);
                        } else {
                            p.nj.getPlace().chatNPC(p, npcId, "Không có thư để con giao");
                        }
                    }
                    break;
                }
                case 17: {
                    val jaien = Ninja.getNinja("Jaian");
                    jaien.p = new User();
                    jaien.p.nj = jaien;
                    val place = p.nj.getPlace();
                    jaien.upHP(jaien.getMaxHP());
                    jaien.isDie = false;

                    jaien.x = place.map.template.npc[0].x;
                    jaien.id = -p.nj.id;
                    jaien.y = place.map.template.npc[0].y;
                    place.Enter(jaien.p);
                    Place.sendMapInfo(jaien.p, place);
                    break;
                }
                case 18: {
                    switch (menuId) {
                        case 0: { int value = util.nextInt(1);
                            if (value == 0) {
                                p.nj.getPlace().chatNPC(p, (short) 18, "Đây là Làng Chài, do Chíp quản lý !");
                            }
                            break;
                        }
                    }
                    break;
                }
                case 19: {
                    if (menuId == 0) {
                        if (p.nj.exptype == 0) {
                            p.nj.exptype = 1;
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Đã tắt không nhận kinh nghiệm");
                            break;
                        }
                        p.nj.exptype = 0;
                        p.nj.getPlace().chatNPC(p, (short) npcId, "Đã bật không nhận kinh nghiệm");
                        break;
                    } else if (menuId == 1){
                        p.passold = "";
                        this.sendWrite(p.nj, (short)51, "Nhập mật khẩu cũ");
                        break;
                    } else if (menuId == 2){
                        if (!p.nj.name.equals("admin")) {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Chỉ admin mới sử dụng được chức năng này");
                            break;
                        } else {
                            this.sendWrite(p.nj, (short) 53, "Nhập tên nhân vật gửi đồ");
                            break;
                        }
                    }
                    break;
                }
                case 22: {
                      p.session.sendMessageLog("Chức Năng Đang Bảo Trì !");
//                    if (menuId != 0) {
//                        break;
//                    }
//                    if (p.nj.clan.clanName.isEmpty()) {
//                        p.nj.getPlace().chatNPC(p, (short) npcId, "Con cần phải có gia tộc thì mới có thể điểm danh được nhé");
//                        break;
//                    }
//                    if (p.nj.ddClan) {
//                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hôm nay con đã điểm danh rồi nhé, hãy quay lại đây vào ngày mai");
//                        break;
//                    }
//                    p.nj.ddClan = true;
//                    final ClanManager clan = ClanManager.getClanByName(p.nj.clan.clanName);
//                    if (clan == null) {
//                        p.nj.getPlace().chatNPC(p, (short) npcId, "Gia tộc lỗi");
//                        return;
//                    }
//                    p.upExpClan(util.nextInt(500, 1000) * clan.getLevel());
//                    p.upluongMessage(50 * clan.getLevel());
//                    p.nj.upyenMessage(500000 * clan.getLevel());
//                    p.nj.getPlace().chatNPC(p, (short) npcId, "Điểm danh mỗi ngày sẽ nhận được các phần quà giá trị");
                    break;
                }
                case 25: {
                    switch (menuId) {
                        case 0: {
                            p.typemenu = 103;
                            doMenuArray(p, new String[]{"Cấp 60", "Cấp 70", "Cấp 80", "Cấp 90", "Cấp 100"});
                            break;
                        }
                        case 1: {
                            switch (optionId) {
                                case 0: {
                                    // Nhiem vu hang ngay
                                    if (p.nj.getLevel() < 20) {
                                        p.session.sendMessageLog("Yêu cầu trình độ cấp 20");
                                        return;
                                    }
                                    if (p.nj.getTasks()[NHIEM_VU_HANG_NGAY] == null && p.nj.nvhnCount < 20) {
                                        val task = createTask(p.nj.getLevel());
                                        if (task != null) {
                                            p.nj.addTaskOrder(task);
                                        } else {
                                            p.nj.getPlace().chatNPC(p, (short) 25, "Nhiệm vụ lần này có chút trục trặc chắc con không làm được rồi ahihi");
                                        }
                                    } else if (p.nj.nvhnCount >= 20) {
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Hôm nay con đã làm hết nhiệm vụ ta giao. Hãy quay lại vào ngày hôm sau.");
                                    } else {
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Nhiệm vụ lần trước ta giao cho con vẫn chưa hoàn thành.");
                                    }
                                    break;
                                }
                                case 1: {
                                    // Huy nhiem vu
                                    p.nj.huyNhiemVu(NHIEM_VU_HANG_NGAY);
                                    break;
                                }
                                case 2: {
                                    // Hoan thanh
                                    if (!p.nj.hoanThanhNhiemVu(NHIEM_VU_HANG_NGAY)) {
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Hãy hoàn thành nhiệm vụ trước rồi đến gặp ta nhận thưởng.");
                                    } else {
                                        // TODO nhan qua NVHN
                                        p.upExpClan(util.nextInt(10, 20));//
                                        p.nj.upyenMessage(util.nextInt(MIN_YEN_NVHN * 50, MAX_YEN_NVHN * 100));
                                        if ((p.nj.getTaskId() == 30 && p.nj.getTaskIndex() == 1) ||
                                                (p.nj.getTaskId() == 39 && p.nj.getTaskIndex() == 3)) {
                                            p.nj.upMainTask();
                                        }
                                    }
                                    break;
                                }

                                case 3: {
                                    // Di toi
                                    if (p.nj.getTasks() != null &&
                                            p.nj.getTasks()[NHIEM_VU_HANG_NGAY] != null
                                    ) {
                                        val task = p.nj.getTasks()[NHIEM_VU_HANG_NGAY];
                                        val map = Server.getMapById(task.getMapId());
                                        p.nj.setMapid(map.id);
                                        for (Npc npc : map.template.npc) {
                                            if (npc.id == 13) {
                                                p.nj.x = npc.x;
                                                p.nj.y = npc.y;
                                                p.nj.getPlace().leave(p);
                                                map.getFreeArea().Enter(p);
                                                break;
                                            }
                                        }
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Nhiệm vụ lần này gặp lỗi con hãy đi up level lên đi rồi nhận lại nhiệm vụ từ ta");
                                    } else {
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Hãy nhận nhiệm vụ mỗi ngày từ ta rồi mới sử dụng tính năng này.");
                                    }
                                }
                            }
                            break;
                        }
                        case 2: {
                            // Ta thu
                            switch (optionId) {
                                case 0: {
                                    //Nhan nhiem vu
                                    if (p.nj.getLevel() < 30) {
                                        p.session.sendMessageLog("Yêu cầu trình độ cấp 30");
                                        return;
                                    }
                                    if (p.nj.getTasks()[NHIEM_VU_TA_THU] == null) {
                                        if (p.nj.taThuCount > 0) {
                                            val task = createBeastTask(p.nj.getLevel());
                                            if (task != null) {
                                                p.nj.addTaskOrder(task);
                                            } else {
                                                p.nj.getPlace().chatNPC(p, (short) 25, "Nhiệm vụ lần này có chút trục trặc chắc con không làm được rồi ahihi");
                                            }
                                        } else {
                                            p.nj.getPlace().chatNPC(p, (short) 25, "Hôm nay con đã làm hết nhiệm vụ ta giao. Hãy quay lại vào ngày hôm sau.");
                                        }
                                    } else {
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Nhiệm vụ lần trước ta giao cho con vẫn chưa hoàn thành.");
                                    }
                                    break;
                                }
                                case 1: {
                                    p.nj.huyNhiemVu(NHIEM_VU_TA_THU);
                                    break;
                                }
                                case 2: {
                                    if (!p.nj.hoanThanhNhiemVu(NHIEM_VU_TA_THU)) {
                                        p.nj.getPlace().chatNPC(p, (short) 25, "Hãy hoàn thành nhiệm vụ trước rồi đến gặp ta nhận thưởng.");
                                    } else {
                                        val i = ItemData.itemDefault(251);
                                        i.quantity = p.nj.get().getLevel() >= 60 ? 5 : 2;
                                        p.nj.addItemBag(true, i);
                                        if ((p.nj.getTaskId() == 30 && p.nj.getTaskIndex() == 2) || (p.nj.getTaskId() == 39 && p.nj.getTaskIndex() == 1)) {
                                            p.nj.upyenMessage(util.nextInt(MIN_YEN_NVHN * 80, MAX_YEN_NVHN * 150));
                                            p.nj.upMainTask();
                                        }
                                        p.upExpClan(util.nextInt(10, 20));
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case 3: {
                            // Chien truong
                            switch (optionId) {
                                case 0: {
                                    // bach
                                    p.nj.enterChienTruong(IBattle.CAN_CU_DIA_BACH);
                                    break;
                                }
                                case 1: {
                                    // hac gia
                                    p.nj.enterChienTruong(IBattle.CAN_CU_DIA_HAC);
                                    break;
                                }
                                case 2: {
                                    Service.sendBattleResult(p.nj, Server.getInstance().globalBattle);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
                case 26: {
                    if (menuId == 0) {
                        p.openUI(14);
                        break;
                    }
                    if (menuId == 1) {
                        p.openUI(15);
                        break;
                    }
                    if (menuId == 2) {
                        p.openUI(32);
                        break;
                    }
                    if (menuId == 3) {
                        p.openUI(34);
                        break;
                    }
                    break;
                }
                case 30: {
                    switch (menuId) {
                        case 0: {
                            p.openUI(38);
                            break;
                        }
                        case 1: {
                            this.sendWrite(p.nj, (short)46, "Nhập mã quà tặng");
                            break;
                        }
                        case 2: {
                            if (optionId == 0) {
                                this.server.manager.rotationluck[0].luckMessage(p);
                                break;
                            }
                            if (optionId == 2) {
                                this.server.manager.sendTB(p, "Vòng xoay vip", "Tham gia đi xem luật lm gì");
                                break;
                            }
                            break;
                        }
                        case 3: {
                            if (optionId == 0) {
                                this.server.manager.rotationluck[1].luckMessage(p);
                                break;
                            }
                            if (optionId == 2) {
                                this.server.manager.sendTB(p, "Vòng xoay thường", "Tham gia đi xem luật lm gì");
                                break;
                            }
                            break;
                        }
                    }
                    break;
                }
                case 32: {
                    switch (menuId) {
                        case 0: {
                            switch (optionId) {
                                case 0: {
                                    // Chien truong keo Tham gia
//                                    Server.candyBattleManager.enter(p.nj);
//                                    break;
                                }
                                case 1: {
                                    // Chien truong keo huong dan
                                    Service.sendThongBao(p.nj, "Chiến trường kẹo:\n" +
                                            "\t- 20 ninja sẽ chia làm 2 đội Kẹo Trăng và Kẹo Đen.\n" +
                                            "\t- Mỗi đội chơi sẽ có nhiệm vụ tấn công giở kẹo của đối phương, nhặt kẹo và sau đó chạy về bỏ vào giỏ kẹo của bên đội mình.\n" +
                                            "\t- Trong khoảng thời gian ninja giữ kẹo sẽ bị mất một lượng HP nhất định theo thời gian.\n" +
                                            "\t- Giữ càng nhiều thì nguy hiểm càng lớn.\n" +
                                            "\t- Còn 10 phú cuối cùng sẽ xuất hiện Phù Thuỷ");
                                    break;
                                }
                            }
                            break;
                        }
                        case 1: {
                            // Option 1
                            val clanManager = ClanManager.getClanByName(p.nj.clan.clanName);
                            if (clanManager != null) {
                                // Có gia tọc và khong battle
                                if (clanManager.getClanBattle() == null) {
                                    //  Chua duoc moi battle
                                    if (p.nj.getClanBattle() == null) {
                                        // La toc truong thach dau
                                        if (p.nj.clan.typeclan == TOC_TRUONG) {
                                            if (clanManager.getClanBattleData() == null ||
                                                    (clanManager.getClanBattleData() != null && clanManager.getClanBattleData().isExpired())) {
                                                sendWrite(p.nj, (byte) 4, "Nhập vào gia tộc muốn chiến đấu");
                                            } else {
                                                if (clanManager.restore()) {
                                                    enterClanBattle(p, clanManager);
                                                } else {
                                                    p.nj.getPlace().chatNPC(p, (short) 32, "Không hỗ trợ");
                                                }
                                            }
                                        } else {
                                            // Thử tìm battle data
                                            p.nj.getPlace().chatNPC(p, (short) 32, "Không hỗ trợ");
                                        }
                                    }
                                } else {
                                    enterClanBattle(p, clanManager);
                                }
                            }
                            break;
                        }
                        case 4: {
                            if (optionId == 0) {
                                p.openUI(43);
                            } else if (optionId == 1) {
                                p.openUI(44);
                                break;
                            } else if (optionId == 2) {
                                p.openUI(45);
                                break;
                            }
                            break;
                        }
                    }
                    break;
                }
                case 33: {
                    if (p.typemenu != 33) {
                        break;
                    }
                    switch (this.server.manager.EVENT) {
                        case 1: {
                            switch (menuId) {
                                case 0: {
                                    if (p.nj.quantityItemyTotal(432) < 1 || p.nj.quantityItemyTotal(428) < 3 || p.nj.quantityItemyTotal(429) < 2 || p.nj.quantityItemyTotal(430) < 3) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(434);
                                    p.nj.addItemBag(true, it);
                                    p.nj.removeItemBags(432, 1);
                                    p.nj.removeItemBags(428, 3);
                                    p.nj.removeItemBags(429, 2);
                                    p.nj.removeItemBags(430, 3);
                                    break;
                                }
                                case 1: {
                                    if (p.nj.quantityItemyTotal(433) < 1 || p.nj.quantityItemyTotal(428) < 2 || p.nj.quantityItemyTotal(429) < 3 || p.nj.quantityItemyTotal(431) < 2) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(435);
                                    p.nj.addItemBag(true, it);
                                    p.nj.removeItemBags(433, 1);
                                    p.nj.removeItemBags(428, 2);
                                    p.nj.removeItemBags(429, 3);
                                    p.nj.removeItemBags(431, 2);
                                    break;
                                }
                            }
                            break Label_6355;
                        }
                        case 2: {
                            switch (menuId) {
                                case 0: {
                                    if (p.nj.quantityItemyTotal(304) < 1 || p.nj.quantityItemyTotal(298) < 1 || p.nj.quantityItemyTotal(299) < 1 || p.nj.quantityItemyTotal(300) < 1 || p.nj.quantityItemyTotal(301) < 1) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(302);
                                    p.nj.addItemBag(true, it);
                                    p.nj.removeItemBags(304, 1);
                                    p.nj.removeItemBags(298, 1);
                                    p.nj.removeItemBags(299, 1);
                                    p.nj.removeItemBags(300, 1);
                                    p.nj.removeItemBags(301, 1);
                                    break;
                                }
                                case 1: {
                                    if (p.nj.quantityItemyTotal(305) < 1 || p.nj.quantityItemyTotal(298) < 1 || p.nj.quantityItemyTotal(299) < 1 || p.nj.quantityItemyTotal(300) < 1 || p.nj.quantityItemyTotal(301) < 1) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(303);
                                    p.nj.addItemBag(true, it);
                                    p.nj.removeItemBags(305, 1);
                                    p.nj.removeItemBags(298, 1);
                                    p.nj.removeItemBags(299, 1);
                                    p.nj.removeItemBags(300, 1);
                                    p.nj.removeItemBags(301, 1);
                                    break;
                                }
                                case 2: {
                                    if (p.nj.yen < 10000 || p.nj.quantityItemyTotal(292) < 3 || p.nj.quantityItemyTotal(293) < 2 || p.nj.quantityItemyTotal(294) < 3) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu hoặc yên");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(298);
                                    p.nj.addItemBag(true, it);
                                    p.nj.upyenMessage(-10000L);
                                    p.nj.removeItemBags(292, 3);
                                    p.nj.removeItemBags(293, 2);
                                    p.nj.removeItemBags(294, 3);
                                    break;
                                }
                                case 3: {
                                    if (p.nj.yen < 10000 || p.nj.quantityItemyTotal(292) < 2 || p.nj.quantityItemyTotal(295) < 3 || p.nj.quantityItemyTotal(294) < 2) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu hoặc yên");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(299);
                                    p.nj.addItemBag(true, it);
                                    p.nj.upyenMessage(-10000L);
                                    p.nj.removeItemBags(292, 2);
                                    p.nj.removeItemBags(295, 3);
                                    p.nj.removeItemBags(294, 2);
                                    break;
                                }
                                case 4: {
                                    if (p.nj.yen < 10000 || p.nj.quantityItemyTotal(292) < 2 || p.nj.quantityItemyTotal(295) < 3 || p.nj.quantityItemyTotal(297) < 3) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu hoặc yên");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(300);
                                    p.nj.addItemBag(true, it);
                                    p.nj.upyenMessage(-10000L);
                                    p.nj.removeItemBags(292, 2);
                                    p.nj.removeItemBags(295, 3);
                                    p.nj.removeItemBags(297, 3);
                                    break;
                                }
                                case 5: {
                                    if (p.nj.yen < 10000 || p.nj.quantityItemyTotal(292) < 2 || p.nj.quantityItemyTotal(296) < 2 || p.nj.quantityItemyTotal(297) < 3) {
                                        p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ nguyên liệu hoặc yên");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        break;
                                    }
                                    final Item it = ItemData.itemDefault(301);
                                    p.nj.addItemBag(true, it);
                                    p.nj.upyenMessage(-10000L);
                                    p.nj.removeItemBags(292, 2);
                                    p.nj.removeItemBags(296, 2);
                                    p.nj.removeItemBags(297, 3);
                                    break;
                                }
                            }
                            break Label_6355;
                        }
                        default: {
                            p.nj.getPlace().chatNPC(p, (short) npcId, "Hiện tại chưa có sự kiện diễn ra");
                            break Label_6355;
                        }
                    }
                }

                case 34: {
                    switch (menuId) {
                        case 0: {
                            if (p.nj.quantityItemyTotal(673) < 1) {
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Hành trang của con không có đủ quà trang trí");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            int a = util.nextInt(10);
                            if (a < 3) {
                                final int num = util.nextInt(10000,20000);
                                p.nj.upyenMessage(num);
                                p.sendYellowMessage("Bạn nhận được " + num + " yên");
                            } else if (a < 6) {
                                p.updateExp(1500000, false);
                            } else {
                                final short[] arId = {3,4,5,6,7,652,653,654,655,449,450,652,653,654,655,451,452,453,449,450,451,452,453, 8,9,10,652,653,654,655,11,449,652,652,653,654,655,653,654,655,450,451,452,453,30,652,653,654,655,249,250,449,450,451,452,453,3,4,5,6,7,275,276,3,4,5,6,7,277,3,4,5,6,7,278,3,4,5,6,7,283,3,4,5,6,7,375,3,4,5,6,7,376,377,3,4,5,6,7,378,449,450,451,452,453,449,450,451,452,453,379,3,4,449,450,451,452,453,449,450,451,449,450,451,452,453,449,450,451,452,453,452,453,5,6,7,380,409,3,4,5,6,7,3,4,5,6,7,410,436,3,4,5,6,7,3,4,5,6,7,437,438,3,4,5,6,7,449,450,451,452,453,449,450,451,452,453,3,4,5,6,7,3,4,5,6,7,3,4,449,450,451,452,453,449,450,451,452,453,5,6,7,449,450,451,3,4,5,6,7,3,4,5,6,7,452,453,3,4,5,6,7,3,4,5,6,7,454,3,4,5,6,7,3,4,5,6,7,3,4,5,6,7,3,4,5,6,7,545,567,3,4,5,6,7,3,4,5,6,7,568,3,4,5,6,7,3,4,5,6,7,570,571,3,4,5,6,7,3,4,5,6,7,573,574,575,3,4,5,6,7,3,4,5,6,7,576,577,3,4,5,6,7,449,450,451,452,453,449,450,451,452,453,3,4,5,6,449,450,451,452,453,449,450,451,452,453,7,578,695,696,3,4,5,449,450,451,452,453,449,450,451,452,453,6,7,3,4,5,6,7,3,4,5,6,7,3,4,5,6,7,775,3,4,5,6,7,3,4,5,6,7,778,779,3,4,5,6,7,3,4,5,6,7,788,789};
                                final short idI = arId[util.nextInt(arId.length)];
                                final ItemData data2 = ItemData.ItemDataId(idI);
                                Item itemup;
                                if (data2.type < 10) {
                                    if (data2.type == 1) {
                                        itemup = ItemData.itemDefault(idI);
                                        itemup.sys = GameScr.SysClass(data2.nclass);
                                    } else {
                                        final byte sys = (byte) util.nextInt(1, 3);
                                        itemup = ItemData.itemDefault(idI, sys);
                                    }
                                } else {
                                    itemup = ItemData.itemDefault(idI);
                                }
                                itemup.setLock(false);
                                p.nj.addItemBag(false, itemup);
                            }
                            p.nj.removeItemBags(673, 1);
                            p.nj.diemsk1 += 1;
                            break;
                        }
                    }
                    break;
                }
                case 37: {
                    switch (menuId) {
                        case 0: {
                            
                            if (p.nj.rewardtt > 0) {
                                p.session.sendMessageLog("Bạn đã nhận quà tân thủ rồi.");
                                break;
                            }else if (p.nj.getAvailableBag() < 4) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            }else {
                                Item item = ItemData.itemDefault(248);//Nấm Linh Chi
                                item.setLock(true);
                                item.isExpires = false;
                                item.expires = -1;
                                p.nj.addItemBag(false, item);

                                item  = ItemData.itemDefault(248);//Nấm Linh Chi
                                item.setLock(true);
                                item.isExpires = false;
                                item.expires = -1;
                                p.nj.addItemBag(false, item);
                                item  = ItemData.itemDefault(194);//Kiếm Gỗ
                                item.setLock(false);
                                item.isExpires = false;
                                item.expires = -1;
                                p.nj.addItemBag(false, item);
                                item  = ItemData.itemDefault(215);//Túi Vải C1
                                item.setLock(true);
                                item.isExpires = false;
                                item.expires = -1;
                                p.nj.addItemBag(false, item);
                                item  = ItemData.itemDefault(229);//Túi Vải C2
                                item.setLock(true);
                                item.isExpires = false;
                                item.expires = -1;
                                p.nj.addItemBag(false, item);
                                item  = ItemData.itemDefault(283);//Túi Vải C3
                                item.setLock(true);
                                item.isExpires = false;
                                item.expires = -1;
                                p.nj.addItemBag(false, item);
                                p.upluongMessage(2000L);
                                p.nj.upyenMessage(20000000L);
                                p.nj.upxuMessage(1000000L);
                                p.nj.rewardtt++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                        case 1: {
                            
                            if (p.nj.getLevel() < 30) {
                                p.session.sendMessageLog("Yêu cầu level 30 để nhận.");
                                break;
                            }
                            if (p.nj.rewardtt30 > 0) {
                                p.session.sendMessageLog("Bạn đã nhận quà hỗ trợ level 30 rồi.");
                                break;
                            }else if (p.nj.getAvailableBag() < 4) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            } else {
                                Item item = ItemData.itemDefault(248);//nấm linh chi
                                item.setLock(true);
                                p.nj.addItemBag(false, item);
                                item = ItemData.itemDefault(37);//vô hạn khả di lệnh
                                item.setLock(true);
                                item.isExpires = true;
                                item.expires = System.currentTimeMillis() + 604800000L;
                                p.nj.addItemBag(false, item);
                                for (byte n = 0; n < 3; ++n) {
                                    item = ItemData.itemDefault(7);//đá cấp 8
                                    item.setLock(true);
                                    p.nj.addItemBag(false, item);
                                }
                                for (int n = 0; n < 500; ++n) {
                                    item = ItemData.itemDefault(15);//hp vừa
                                    item.setLock(true);
                                    p.nj.addItemBag(true, item);
                                    item = ItemData.itemDefault(20);//mp vừa
                                    item.setLock(true);
                                    p.nj.addItemBag(true, item);//mp vừa
                                }
                                p.nj.rewardtt30++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                        case 2: {
                            p.nj.getPlace().chatNPC(p, 37, "Ta Xin Lỗi Vì Phải Bảo trì chức năng , nhưng mà dm nó lỗi quá con à");
//                            if (p.ddhn > 0) {
//                                p.session.sendMessageLog("Ban da diem danh roi.");
//                                return;
//                            }
//                            if (p.nj.diemdungluong < 30000) {
//                                p.session.sendMessageLog("Cần 30.000 Điểm Tiêu Sài mới điểm danh được.");
//                                return;
//                            }
//                            p.ddhn = 1;
//                            p.upluongMessage(500);
//                            p.session.sendMessageLog("Điểm Danh Thành Công !");
                            break;
                        }
                        case 3: {
                            this.server.manager.sendTB(p, "Top câu cá", BXHManager.getStringBXH(4));
                            break;
                        }
                        case 4: {
                            this.server.manager.sendTB(p, "Điểm Sài Lượng", BXHManager.getStringBXH(6));
                            break;
                        }
                        case 5: {
                        if (p.nj.luong < 10000){
                        p.session.sendMessageLog("bạn không đủ 10.000 lượng để đổi");
                        return;
                        }
                        p.upluongMessage(-10000);

                        p.nj.pointUydanh += 50;
                        p.nj.pointNon    += 50;
                        p.nj.pointVukhi += 50;
                        p.nj.pointAo += 50;
                        p.nj.pointLien += 50;
                        p.nj.pointGangtay += 50;
                        p.nj.pointNhan += 50;
                        p.nj.pointQuan += 50;
                        p.nj.pointNgocboi += 50;
                        p.nj.pointGiay += 50;
                        p.nj.pointPhu += 50;
                            return;
                        }
                        case 6: {
                            this.server.manager.sendTB(p, "Top Thả Đèn", BXHManager.getStringBXH(7));
                            break;
                        }
                    }
                    break;
                }
                case 92: {
                    p.typemenu = ((menuId == 0) ? 93 : 94);
                    this.doMenuArray(p, new String[]{"Thông tin", "Luật chơi"});
                    break;
                }
                case 93: {
                    if (menuId == 0) {
                        this.server.manager.rotationluck[0].luckMessage(p);
                        break;
                    }
                    if (menuId == 1) {
                        this.server.manager.sendTB(p, "Vòng xoay vip", "Tham gia đi xem luật lm gì");
                        break;
                    }
                    break;
                }
                case 94: {
                    if (menuId == 0) {
                        this.server.manager.rotationluck[1].luckMessage(p);
                        break;
                    }
                    if (menuId == 1) {
                        this.server.manager.sendTB(p, "Vòng xoay thường", "Tham gia đi xem luật lm gì");
                        break;
                    }
                    break;
                }
                case 95: {
                    break;
                }
                //thưởng thăng cấp 60-100
                case 103: {
                    switch (menuId) {
                        case 0: {
                            if (p.nj.getLevel() < 60) {
                                p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                break;
                            }
                            if (p.nj.getAvailableBag() < 1) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            }
                            if (p.nj.reward60 > 0) {
                                p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                break;
                            } else {
                                p.upluongMessage(50L);
                                p.nj.reward60++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                        case 1: {
                            if (p.nj.getLevel() < 70) {
                                p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                break;
                            }
                            if (p.nj.getAvailableBag() < 1) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            }
                            if (p.nj.reward70 > 0) {
                                p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                break;
                            } else {
                                p.upluongMessage(50L);
                                p.nj.reward70++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                        case 2: {
                            if (p.nj.getLevel() < 80) {
                                p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                break;
                            }
                            if (p.nj.getAvailableBag() < 1) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            }
                            if (p.nj.reward80 > 0) {
                                p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                break;
                            } else {
                                p.upluongMessage(50L);
                                p.nj.reward80++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                        case 3: {
                            if (p.nj.getLevel() < 90) {
                                p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                break;
                            }
                            if (p.nj.getAvailableBag() < 1) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            }
                            if (p.nj.reward90 > 0) {
                                p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                break;
                            } else {
                                p.upluongMessage(50L);
                                p.nj.reward90++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                        case 4: {
                            if (p.nj.getLevel() < 100) {
                                p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                break;
                            }
                            if (p.nj.getAvailableBag() < 1) {
                                p.session.sendMessageLog("Hành trang đầy.");
                                break;
                            }
                            if (p.nj.reward100 > 0) {
                                p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                break;
                            } else {
                                p.upluongMessage(50L);
                                p.nj.reward100++;
                                p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                            }
                            break;
                        }
                    }
                    break;
                }
                case 120: {
                    if (menuId > 0 && menuId < 7) {
                        p.Admission(menuId);
                        break;
                    }
                    break;
                }
                case 23: {
                    // Matsurugi
                    if (ninja.getTaskId() == 23 && ninja.getTaskIndex() == 1 && menuId == 0) {
                        boolean hasItem = false;
                        for (Item item : p.nj.ItemBag) {
                            if (item != null && item.id == 230) {
                                hasItem = true;
                                break;
                            }
                        }

                        if (!hasItem) {
                            val i = ItemData.itemDefault(230);
                            i.setLock(true);
                            p.nj.addItemBag(false, i);
                            p.nj.getPlace().chatNPC(p, 23, "Ta hi vọng đây là lần cuối ta giao chìa khoá cho con ta nghĩ lần này con sẽ làm được. ");
                        } else {
                            p.nj.getPlace().chatNPC(p, 23, "Con đã có chìa khoá rồi không thể nhận thêm được");
                        }
                    } else {
                        p.nj.getPlace().chatNPC(p, 23, "Ta không quen biết con con đi ra đi");
                    }
                    break;
                }
                case 20: {
                    // Soba
                    if (menuId == 0) {
                        if (!ninja.hasItemInBag(266)) {
                            if (ninja.getTaskId() == 32 && ninja.getTaskIndex() == 1) {
                                val item = ItemData.itemDefault(266);
                                item.setLock(true);
                                ninja.addItemBag(false, item);
                            }
                        } else {
                            ninja.p.sendYellowMessage("Con đã có cần câu không thể nhận thêm");
                        }
                    } else {
                        ninja.getPlace().chatNPC(ninja.p, 20, "Làng ta rất thanh bình con có muốn sống ở đây không");
                    }
                    break;
                }
                case 28: {
                    // Shinwa
                    switch (menuId) {
                        case 0: {
                            final List<ItemShinwa> itemShinwas = items.get((int) optionId);
                            Message mess = new Message(103);
                            mess.writer().writeByte(optionId);
                            if (itemShinwas != null) {
                                mess.writer().writeInt(itemShinwas.size());
                                for (ItemShinwa item : itemShinwas) {
                                    val itemStands = item.getItemStand();
                                    mess.writer().writeInt(itemStands.getItemId());
                                    mess.writer().writeInt(itemStands.getTimeEnd());
                                    mess.writer().writeShort(itemStands.getQuantity());
                                    mess.writer().writeUTF(itemStands.getSeller());
                                    mess.writer().writeInt(itemStands.getPrice());
                                    mess.writer().writeShort(itemStands.getItemTemplate());
                                }
                            } else {
                                mess.writer().writeInt(0);
                            }
                            mess.writer().flush();
                            p.sendMessage(mess);
                            mess.cleanup();
                            break;
                        }
                        case 1: {
                            // Sell item
                            p.openUI(36);
                            break;
                        }
                        case 2: {
                            // Get item back

                            for (ItemShinwa itemShinwa : items.get(-2)) {
                                if (p.nj.getAvailableBag() == 0) {
                                    p.sendYellowMessage("Hành trang không đủ ô trống để nhận thêm");
                                    break;
                                }
                                if (itemShinwa != null) {
                                    if (p.nj.name.equals(itemShinwa.getSeller())) {
                                        itemShinwa.item.quantity = itemShinwa.getQuantity();
                                        p.nj.addItemBag(true, itemShinwa.item);
                                        items.get(-2).remove(itemShinwa);
                                        deleteItem(itemShinwa);
                                    }
                                }
                            }

                            break;
                        }
                        case 3: {
                            p.indexMenuBox = 0;
                            Service.openMenuMuaLai(p);
                            break;
                        }
                    }
                    break;
                }
                case 27: {
                    // Cam chia khoa co quan
                    if (Arrays.stream(p.nj.ItemBag).anyMatch(item -> item != null && (item.id == 231 || item.id == 260))) {
                        p.nj.removeItemBags(231, 1);
                        p.nj.removeItemBags(260, 1);
                        p.getClanTerritoryData().getClanTerritory().plugKey(p.nj.getMapid(), p.nj);

                    } else {
//                        p.nj.addItemBag(true, ItemData.itemDefault(260));
                        p.sendYellowMessage("Không có chìa khoá để cắm");
                    }
                    break;
                }

                case 24: {
                   switch (menuId) {
                        case 0: {
                            if (p.nj.diemdungluong < 1000){
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Bạn Cần 1k Điểm Dùng Lượng Để đổi Lượng qua xu");
                                break;
                            }
                            if (optionId == 0) {
                                if (p.nj.luong < 5000) {
                                    p.session.sendMessageLog("Bạn không đủ lượng.");
                                    break;
                                }
                                p.upluongMessage(-5000);
                                p.nj.upxuMessage(2000000L);
                                LogHistory.log3(p.nj.name + " da doi 50 lg ra 20000000 xu");
                                break;
                            } else if (optionId == 1) {
                                 if (p.nj.diemdungluong < 2000){
                                p.nj.getPlace().chatNPC(p, (short) npcId, "Bạn Cần 1k Điểm Dùng Lượng Để đổi Lượng qua yen");
                                break;
                            }
                                if (p.nj.luong < 5000) {
                                    p.session.sendMessageLog("Bạn không đủ lượng.");
                                    break;
                                }
                                p.upluongMessage(-5000);
                                p.nj.upyenMessage(2500000L);
                                LogHistory.log3(p.nj.name + " da doi 5000 lg ra 25000000 yen");
                                break;
                            }
                        }
                        case 1: {
//                            if (p.nj.diemhd < 20) {
//                                p.nj.getPlace().chatNPC(p, (short)npcId, "Cần 20 điểm hoạt động để đổi yên qua xu");
//                                break;
//                            }
//                            if (p.nj.yen < 500000) {
//                                p.nj.getPlace().chatNPC(p, (short)npcId, "Bạn không có đủ yên");
//                                break;
//                            }
//                            p.nj.upyenMessage(-5000000L);
//                            p.nj.upxuMessage(100000L);
//                            p.nj.diemhd -= 20;
//                            p.sendYellowMessage("Bạn đã đổi yên ra xu thành công");
                            break;
                        }
                        case 2: {
                            if (optionId == 1) {
                                this.server.manager.sendTB(p, "Bang gia", "1 :10.000 = 5.000 lượng \n" + 
                                        "2 : 50.0000 = 25.000 lượng \n" + 
                                        "3 : 100.0000 = 50.000 lượng \n" + 
                                        "4 : 200.000 = 100.000 lượng \n" + 
                                        "5 : 500.000 = 250.000 lượng \n" + 
                                        "6 : 1.000.000 = 500.000 lượng");
                                break;
                            }
                            break;
                        }
                        case 3: {
                            switch (optionId) {
                                case 0: {
                                    if (p.nj.getLevel() < 10) {
                                        p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() < 1) {
                                        p.session.sendMessageLog("Hành trang đầy.");
                                        break;
                                    }
                                    if (p.nj.reward10 > 0) {
                                        p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                        break;
                                    } else {
                                        Item item = ItemData.itemDefault(248);//nấm linh chi // thêm vật phẩm nhận thưởng
                                        item.setLock(true);
                                        p.nj.addItemBag(false, item);
                                        p.upluongMessage(10L);
                                        p.nj.reward10++;
                                        p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                                    }
                                    break;
                                }
                                case 1: {
                                    if (p.nj.getLevel() < 20) {
                                        p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() < 1) {
                                        p.session.sendMessageLog("Hành trang đầy.");
                                        break;
                                    }
                                    if (p.nj.reward20 > 0) {
                                        p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                        break;
                                    } else {
                                        Item item = ItemData.itemDefault(248);//nấm linh chi // thêm vật phẩm nhận thưởng
                                        item.setLock(true);
                                        p.nj.addItemBag(false, item);
                                        p.upluongMessage(20L);
                                        p.nj.reward20++;
                                        p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                                    }
                                    break;
                                }
                                case 2: {
                                    if (p.nj.getLevel() < 30) {
                                        p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() < 1) {
                                        p.session.sendMessageLog("Hành trang đầy.");
                                        break;
                                    }
                                    if (p.nj.reward30 > 0) {
                                        p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                        break;
                                    } else {
                                        Item item = ItemData.itemDefault(248);//nấm linh chi // thêm vật phẩm nhận thưởng
                                        item.setLock(true);
                                        p.nj.addItemBag(false, item);
                                        p.upluongMessage(30L);
                                        p.nj.reward30++;
                                        p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                                    }
                                    break;
                                }
                                case 3: {
                                    if (p.nj.getLevel() < 40) {
                                        p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() < 1) {
                                        p.session.sendMessageLog("Hành trang đầy.");
                                        break;
                                    }
                                    if (p.nj.reward40 > 0) {
                                        p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                        break;
                                    } else {
                                        Item item = ItemData.itemDefault(248);//nấm linh chi // thêm vật phẩm nhận thưởng
                                        item.setLock(true);
                                        p.nj.addItemBag(false, item);
                                        p.upluongMessage(40L);
                                        p.nj.reward40++;
                                        p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                                    }
                                    break;
                                }
                                case 4: {
                                    if (p.nj.getLevel() < 50) {
                                        p.session.sendMessageLog("Bạn chưa đủ cấp độ để nhận thưởng.");
                                        break;
                                    }
                                    if (p.nj.getAvailableBag() < 1) {
                                        p.session.sendMessageLog("Hành trang đầy.");
                                        break;
                                    }
                                    if (p.nj.reward50 > 0) {
                                        p.session.sendMessageLog("Bạn đã nhận thưởng thăng cấp này rồi.");
                                        break;
                                    } else {
                                        Item item = ItemData.itemDefault(248);//nấm linh chi // thêm vật phẩm nhận thưởng
                                        item.setLock(true);
                                        p.nj.addItemBag(false, item);
                                        p.upluongMessage(50L);
                                        p.nj.reward50++;
                                        p.session.sendMessageLog("Nhận thưởng thành công. Vui lòng kiểm tra hành trang.");
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case 4: {
                            this.sendWrite(p.nj, (short)46, "Nhập mã quà tặng");
                            break;
                        }
                        case 5:{
                            int value = util.nextInt(1);
                            if (value == 0) {
                                p.nj.getPlace().chatNPC(p, (short) 24, "Ta là hiện thân của thần tài sẽ mang đến tài lộc đến cho mọi người");
                            }
                            break;
                        }

                    }
                    break;
                }
                case 251: {
                    switch (menuId) {
                        case 0: {
                            if (p.nj.quantityItemyTotal(251) < 250) {
                                p.session.sendMessageLog("Hành trang không đủ 250 Mảnh giấy vụn");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            final Item it = ItemData.itemDefault(252);
                            p.nj.addItemBag(true, it);
                            p.nj.removeItemBags(251, 250);
                            break;
                        }
                        case 1: {
                            if (p.nj.quantityItemyTotal(251) < 300) {
                                p.session.sendMessageLog("Hành trang không đủ 300 Mảnh giấy vụn");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            final Item it = ItemData.itemDefault(253);
                            p.nj.addItemBag(true, it);
                            p.nj.removeItemBags(251, 300);
                            break;
                        }
                    }
                }
                case 572: {
                    switch (menuId) {
                        case 0: {
                            p.typeTBLOption = $240;
                            break;
                        }
                        case 1: {
                            p.typeTBLOption = $480;
                            break;
                        }
                        case 2: {
                            p.typeTBLOption = ALL_MAP;
                            break;
                        }
                        case 3: {
                            p.typeTBLOption = PICK_ALL;
                            break;
                        }
                        case 4: {
                            p.typeTBLOption = USEFUL;
                            break;
                        }
                        case 5: {
                            p.activeTBL = !p.activeTBL;
                        }
                    }
                    break;
                }
                case 40: {
                    switch (menuId) {
                        case 0: {
                            if (p.nj.get().nclass == 0) {
                                p.session.sendMessageLog("Bạn phải vào lớp để sử dụng chức năng này");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            if (p.nj.get().getLevel() < 60) {
                                p.nj.getPlace().chatNPC(p, (short) 40, "Yêu cầu level 60");
                                break;
                            }
                            if (p.nj.luong < 200) {
                                p.session.sendMessageLog("Bạn không có đủ 200 lượng");
                                break;
                            }
                            final Item it = ItemData.itemDefault(396+p.nj.get().nclass);
                            it.setLock(true);
//                            it.isExpires = true;
//                            it.expires = System.currentTimeMillis() + 2592000000L;
                            p.nj.addItemBag(true, it);
                            p.upluongMessage(-200);
                            break;
                        }
                        case 1: {
                            if (p.nj.get().ItemBody[15] == null) {
                                p.session.sendMessageLog("Bạn phải đeo bí kiếp mới có thể luyện bí kiếp");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            if (p.nj.luong < 500) {
                                p.session.sendMessageLog("Bạn không có đủ 500 lượng");
                                break;
                            }
                            final Item it = p.nj.get().ItemBody[15];
                            if (it.getUpgrade() >= 1) {
                                p.session.sendMessageLog("Bí kiếp đã nâng cấp không thể luyện hóa");
                                break;
                            }
                            int a = 0;
                            for (int i = 0; i < GameScr.optionBikiep.length; i++) {
                                if (util.nextInt(1,5) < 3) {
                                    it.option.add(new Option(GameScr.optionBikiep[i],util.nextInt(GameScr.paramBikiep[i],GameScr.paramBikiep[i]*70/100)));
                                    a++;
                                }
                            }
                            it.setLock(true);
                            p.nj.addItemBag(true, it);
                            p.nj.removeItemBody((byte)15);
                            p.upluongMessage(-500);
                            String b = "";
                            if (a > 5) {
                                b = "Khá mạnh đó, ngươi thấy ta làm tốt không ?";
                            } else if (a > 2) {
                                b = "Không tệ, ngươi xem có ổn không ?";
                            } else {
                                b = "Ta chỉ giúp được cho ngươi đến thế thôi. Ta xin lỗi !";
                            }
                            p.nj.getPlace().chatNPC(p, (short)40, b);
                            break;
                        }
                        case 2: {
                            if (p.nj.get().ItemBody[15] == null) {
                                p.session.sendMessageLog("Bạn phải đeo bí kiếp mới có thể nâng cấp bí kiếp");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            if (p.nj.luong < 500) {
                                p.session.sendMessageLog("Bạn không có đủ 500 lượng");
                                break;
                            }
                            Item it = p.nj.get().ItemBody[15];
                            if (it.getUpgrade() >= 16) {
                                p.session.sendMessageLog("Bí kiếp đã đạt cấp tối đa");
                                break;
                            }
                            if (GameScr.percentBikiep[it.getUpgrade()] >= util.nextInt(100)) {
                                for (byte k = 0; k < it.option.size(); ++k) {
                                    final Option option = it.option.get(k);
                                    float dac = option.param * 10 / 100;
                                    if(dac < 1) option.param += 1;
                                    else
                                    option.param += option.param * 10 / 100;
                                }
                                it.setUpgrade(it.getUpgrade()+1);
                                it.setLock(true);
                                p.nj.addItemBag(true, it);
                                p.sendYellowMessage("Nâng cấp thành công!");
                                p.nj.removeItemBody((byte)15);
                            } else {
                                p.sendYellowMessage("Nâng cấp thất bại!");
                            }
                            p.upluongMessage(-500);
                            break;
                        }
                        case 3: {
                            if(p.nj.get().ItemBody[15] == null){
                                p.nj.getPlace().chatNPC(p, (short)40, "Bí kíp không có đòi hủy?");
                                break;
                            }
                            Service.startYesNoDlg(p, (byte) 4, "Con có chắc chắn muốn huỷ bí kíp không?");
                            break;
                        }
                        case 4: {
                            p.nj.getPlace().chatNPC(p, (short)40, "Ta có thể giúp ngươi tăng sức mạnh cho bí kíp, ngươi chỉ cần trả cho ta ít ngân lượng.");
                            break;
                        }
                    }
                    break;
                }
                case 41: {
//                    val lognden = Ninja.getNinja("Lồng đèn");
//                    lognden.p = new User();
//                    lognden.p.nj = lognden;
//                    val place = p.nj.getPlace();
//                    lognden.upHP(lognden.getMaxHP());
//                    lognden.isDie = false;
//                    lognden.isNpc = true;
//                    for (Npc npc : place.map.template.npc) {
//                        if (npc.id == 41) {
//                            lognden.x = npc.x;
//                            lognden.y = npc.y;
//                            break;
//                        }
//                    }
//                    lognden.id = -18;
//                    lognden.masterId = p.nj.id;
//                    place.Enter(lognden.p);
//                    Place.sendMapInfo(lognden.p, place);
                    break;
                }
                default: {
                    p.nj.getPlace().chatNPC(p, (short) npcId, "Chức năng này đang cập nhật nhé");
                    break;
                }
                case 42 :{
//                    p.nj.getPlace().chatNPC(p, (short) npcId, "Chức năng này đang bảo trì nâng cấp , sửa lại !  dự tính 2 ngày sửa xong ! ");
                    switch (menuId) {
                        case 0: {
                            if (p.nj.get().nclass == 0) {
                                p.session.sendMessageLog("Bạn phải vào lớp để sử dụng chức năng này");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            if (p.nj.get().getLevel() < 10) {
                                p.nj.getPlace().chatNPC(p, (short) 42, "Yêu cầu level 10");
                                break;
                            }
                            if (p.nj.luong < 1000) {
                                p.session.sendMessageLog("Bạn không có đủ 1000 lượng");
                                break;
                            }
                            final Item it = ItemData.itemDefault(832);
                            it.setLock(true);
//                            it.isExpires = true;
//                            it.expires = System.currentTimeMillis() + 2592000000L;
                            p.nj.addItemBag(true, it);
                            p.upluongMessage(-1000);
                            break;
                        }
                        case 1: {
                            if (p.nj.get().ItemBody[10] == null) {
                                p.session.sendMessageLog("Bạn phải đeo Pét mới có thể luyện Pet");
                                break;
                            }
                            if (p.nj.get().ItemBody[10].id != 832) {
                                p.session.sendMessageLog("Bạn phải đeo Pét Ứng Long");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            if (p.nj.luong < 1000) {
                                p.session.sendMessageLog("Bạn không có đủ 1000 lượng");
                                break;
                            }
                            final Item it = p.nj.get().ItemBody[10];
                            
                            if (it.getUpgrade() >= 1) {
                                p.session.sendMessageLog("Pet đã nâng cấp không thể luyện hóa");
                                break;
                            }
                            int a = 0;
                            for (int i = 0; i < GameScr.optionPet.length; i++) {
                                if (util.nextInt(5,9) < 8) {
                                    it.option.add(new Option(GameScr.optionPet[i],util.nextInt(GameScr.paramPet[i],GameScr.paramPet[i]*70/100)));
                                    a++;
                                }
                            }
                            it.setLock(true);
                            p.nj.addItemBag(true, it);
                            p.nj.removeItemBody((byte)10);
                            p.upluongMessage(-1000);
                            String b = "";
                            if (a > 5) {
                                b = "Tuyệt Vời ,Khá Mạnh Đó !";
                            } else if (a > 2) {
                                b = "Tuyệt Vời, Con Xem Ổn Không ?";
                            } else {
                                b = "Con Đen Quá ! Ta xin lỗi !";
                            }
                            p.nj.getPlace().chatNPC(p, (short)42, b);
                            break;
                        }
                        case 2: {
                            if (p.nj.get().ItemBody[10] == null) {
                                p.session.sendMessageLog("Bạn phải đeo Pet mới có thể nâng cấp Pét");
                                break;
                            }
                            if (p.nj.get().ItemBody[10].id != 832 ) {
                                p.session.sendMessageLog("Bạn phải đeo Pét Ứng Long");
                                break;
                            }
                            if (p.nj.getAvailableBag() == 0) {
                                p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                break;
                            }
                            if (p.nj.luong < 1000) {
                                p.session.sendMessageLog("Bạn không có đủ 1000 lượng");
                                break;
                            }
                            Item it = p.nj.get().ItemBody[10];
                            if (it.getUpgrade() >= 16) {
                                p.session.sendMessageLog("Pet đã đạt cấp tối đa");
                                break;
                            }
                            if (GameScr.percentPet[it.getUpgrade()] >= util.nextInt(100)) {
                                for (byte k = 0; k < it.option.size(); ++k) {
                                    final Option option = it.option.get(k);
                                    float dac = option.param * 10 / 100;
                                    if(dac < 1) option.param += 1;
                                    else
                                    option.param += option.param * 10 / 100;
                                }
                                it.setUpgrade(it.getUpgrade()+1);
                                it.setLock(true);
                                p.nj.addItemBag(true, it);
                                p.sendYellowMessage("Nâng cấp Pet thành công!");
                                p.nj.removeItemBody((byte)10);
                            } else {
                                p.sendYellowMessage("Nâng cấp Pet thất bại!");
                            }
                            p.upluongMessage(-1000);
                            break;
                        }
                        case 3: {
                           
                            if(p.nj.get().ItemBody[10] == null){
                                p.session.sendMessageLog("Pet không có đòi hủy?");
                                break;
                            }
                            if (p.nj.get().ItemBody[10].id != 832) {
                                p.session.sendMessageLog("Bạn phải đeo Pét Ứng Long");
                                break;
                            }
                            Service.startYesNoDlg(p, (byte) 5, "Con có chắc chắn muốn huỷ Pet ứng long không?");
                            break;
                        }
                        case 4: {
                            p.nj.getPlace().chatNPC(p, (short)42, "Ta có thể giúp ngươi tăng sức mạnh cho Pet, ngươi chỉ cần trả cho ta ít ngân lượng.");
                            break;
                        }
                    }
                    break;
                }
                 case 43:{
                    switch (menuId) {
                    case 0:{
//                    final Manager manager = this.server.manager;
//                    final Map ma = Manager.getMapid(162);
//                    for (final Place area : ma.area) {
//                        if (area.getNumplayers() < ma.template.maxplayers) {
//                            p.nj.getPlace().leave(p);
//                            area.EnterMap0(p.nj);
//                            return;
//                        }
//                        }
                    }
                    break;
                    }
                }
                 case 44: {
                    switch (menuId) {
                        case 0: {
                            switch (optionId) {
                                case 0: {
                                    if (p.nj.quantityItemyTotal(308) < 10) {
                                        p.session.sendMessageLog("Hành trang không đủ 10 Bánh trung thu phong lôi");
                                        return;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        return;
                                    }
                                    final Item it = ItemData.itemDefault(831);
                                    it.isExpires = true;
                                    it.expires = System.currentTimeMillis() + 2592000000L;//30 day
                                    p.nj.addItemBag(true, it);
                                    for (int i = 0; i < 10; i++) {
                                        p.nj.removeItemBags(308, 1);
                                    }
                                    break;
                                }
                                case 1: {
                                    if (p.nj.quantityItemyTotal(308) < 10) {
                                        p.session.sendMessageLog("Hành trang không đủ 10 Bánh trung thu phong lôi");
                                        return;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        return;
                                    }
                                    final Item it = ItemData.itemDefault(800 - p.nj.gender);
                                    it.isExpires = true;
                                    it.expires = System.currentTimeMillis() + 25200000L;//7 day
                                    p.nj.addItemBag(true, it);
                                    for (int i = 0; i < 10; i++) {
                                        p.nj.removeItemBags(308, 1);
                                    }
                                    break;
                                }
                                case 2: {
                                    if (p.nj.quantityItemyTotal(309) < 20) {
                                        p.session.sendMessageLog("Hành trang không đủ 20 Bánh trung thu băng hỏa");
                                        return;
                                    }
                                    if (p.nj.getAvailableBag() == 0) {
                                        p.session.sendMessageLog("Hành trang không đủ chỗ trống");
                                        return;
                                    }
                                    final Item it = ItemData.itemDefault(800 - p.nj.gender);
                                    it.isExpires = true;
                                    it.expires = System.currentTimeMillis() + 2592000000L;//30 day
                                    p.nj.addItemBag(true, it);
                                    for (int i = 0; i < 20; i++) {
                                        p.nj.removeItemBags(309, 1);
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case 1: {
                            switch (optionId) {
                                case 0: {
                                    Service.openMenuNCLD(p, 3);
                                    break;
                                }
                                case 1: {
                                    Service.openMenuNCLD(p, 4);
                                    break;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
//        util.Debug("byte1 " + npcId + " byte2 " + menuId + " byte3 " + optionId);
    }

    private void sendThongBaoTDB(User p, Tournament tournaments, String type) {
        val stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        for (TournamentData tournament : tournaments.getTopTen()) {
            stringBuilder.append(tournament.getRanked())
                    .append(".")
                    .append(tournament.getName())
                    .append("\n");
        }
        Service.sendThongBao(p, stringBuilder.toString());
    }

    public static java.util.Map<Byte, int[]> nangCapMat = new TreeMap<>();

    static {
        nangCapMat.put((byte) 1, new int[]{500, 2_000_000, 80, 200, 100});
        nangCapMat.put((byte) 2, new int[]{400, 3_000_000, 75, 300, 85});
        nangCapMat.put((byte) 3, new int[]{300, 5_000_000, 65, 500, 75});
        nangCapMat.put((byte) 4, new int[]{250, 7_500_000, 55, 700, 65});
        nangCapMat.put((byte) 5, new int[]{200, 8_500_000, 45, 900, 55});
        nangCapMat.put((byte) 6, new int[]{175, 10_000_000, 30, 1000, 45});
        nangCapMat.put((byte) 7, new int[]{150, 12_000_000, 25, 1200, 30});
        nangCapMat.put((byte) 8, new int[]{100, 15_000_000, 20, 1200, 25});
        nangCapMat.put((byte) 9, new int[]{50, 20_000_000, 15, 1500, 20});
    }

    private void nangMat(User p, Item item, boolean vip) throws IOException {

        if (item.id < 694) {
            int toneCount = (int) Arrays.stream(p.nj.ItemBag).filter(i -> i != null && i.id == item.id + 11).map(i -> i.quantity).reduce(0, Integer::sum);
            if (toneCount >= nangCapMat.get(item.getUpgrade())[0]) {

                if (vip && nangCapMat.get(item.getUpgrade())[3] > p.nj.luong) {
                    p.sendYellowMessage("Không đủ lượng nâng cấp vật phẩm");
                    return;
                }
                if (p.nj.xu < nangCapMat.get(item.getUpgrade())[1]) {
                    p.sendYellowMessage("Không đủ xu để nâng cấp");
                    return;
                }
                val succ = util.percent(100, nangCapMat.get(item.getUpgrade())[vip ? 2 : 4]);
                if (succ) {
                    p.nj.get().ItemBody[14] = ItemData.itemDefault(item.id + 1);

                    p.nj.removeItemBags(item.id + 11, nangCapMat.get(item.getUpgrade())[0]);
                    p.sendInfo(false);
                    p.sendYellowMessage("Nâng cấp mắt thành công bạn nhận được mắt " + p.nj.get().ItemBody[14].getData().name + p.nj.get().ItemBody[14].getUpgrade() + " đã mặc trên người");
                } else {
                    p.sendYellowMessage("Nâng cấp mắt thất bại");
                }

                if (vip) {
                    p.removeLuong(nangCapMat.get(item.getUpgrade())[3]);
                }

                p.nj.upxuMessage(-nangCapMat.get(item.getUpgrade())[1]);


            } else {
                p.sendYellowMessage("Không đủ " + nangCapMat.get(item.getUpgrade())[0] + " đá danh vọng cấp " + (item.getUpgrade() + 1) + " để nâng cấp");
            }
        } else {
            p.sendYellowMessage("Mắt được nâng cấp tối đa");
        }
    }

    private void enterClanBattle(User p, ClanManager clanManager) {
        val battle = clanManager.getClanBattle();
        p.nj.setClanBattle(battle);
        if (!clanManager.getClanBattle().enter(p.nj, p.nj.getPhe() == Constants.PK_TRANG ? IBattle.BAO_DANH_GT_BACH :
                IBattle.BAO_DANH_GT_HAC)) {
            p.nj.changeTypePk(Constants.PK_NORMAL);
        }
    }

    public void openUINpc(final User p, Message m) throws IOException {
        final short idnpc = m.reader().readShort();
        m.cleanup();
        p.nj.menuType = 0;
        p.typemenu = idnpc;



        if (idnpc == 33 && server.manager.EVENT != 0) {

            val itemNames = new String[EventItem.entrys.length + 2];

            for (int i = 0; i < itemNames.length - 2; i++) {
                itemNames[i] = EventItem.entrys[i].getOutput().getItemData().name;
            }

            itemNames[EventItem.entrys.length] = "Hướng dẫn";
            itemNames[EventItem.entrys.length+1] = "Top sự kiện";
            createMenu(33, itemNames, "", p);
        }


        if (idnpc == 0 && (p.nj.getPlace().map.isGtcMap() || p.nj.getPlace().map.loiDaiMap())) {
            if (p.nj.hasBattle() || p.nj.getClanBattle() != null) {
                createMenu(idnpc, new String[]{"Đặt cược", "Rời khỏi đây"}, "Con có 5 phút để xem thông tin đối phương", p);
            }

        } else if (idnpc == Manager.ID_EVENT_NPC) {
            createMenu(Manager.ID_EVENT_NPC, Manager.MENU_EVENT_NPC, Manager.EVENT_NPC_CHAT[util.nextInt(0, Manager.EVENT_NPC_CHAT.length - 1)], p);
        } else if ("testgame".equals(p.nj.name) && idnpc == 28) {
            createMenu(28, new String[]{"Bảo trì", "Lưu dữ liệu"}, "Oke", p);
        } else if (idnpc == 32 && (p.nj.getPlace().map.id == IBattle.BAO_DANH_GT_BACH || p.nj.getPlace().map.id == IBattle.BAO_DANH_GT_HAC)) {
            createMenu(idnpc, new String[]{"Tổng kết", "Rời khỏi đây"}, "", p);
        } else {
            val ninja = p.nj;
            val npcTemplateId = idnpc;
            p.nj.menuType = 0;

            String[] captions = null;
            if (TaskHandle.isTaskNPC(ninja, npcTemplateId)) {
                captions = new String[1];
                p.nj.menuType = 1;
                if (ninja.getTaskIndex() == -1) {
                    captions[0] = (TaskList.taskTemplates[ninja.getTaskId()]).name;
                } else if (TaskHandle.isFinishTask(ninja)) {
                    captions[0] = Text.get(0, 12);
                } else if (ninja.getTaskIndex() >= 0 && ninja.getTaskIndex() <= 4 && ninja.getTaskId() == 1) {
                    captions[0] = (TaskList.taskTemplates[ninja.getTaskId()]).name;
                } else if (ninja.getTaskIndex() >= 1 && ninja.getTaskIndex() <= 15 && ninja.getTaskId() == 7) {
                    captions[0] = (TaskList.taskTemplates[ninja.getTaskId()]).name;
                } else if (ninja.getTaskIndex() >= 1 && ninja.getTaskIndex() <= 3 && ninja.getTaskId() == 13) {
                    captions[0] = (TaskList.taskTemplates[ninja.getTaskId()]).name;
                } else if (ninja.getTaskId() >= 11

                ) {
                    captions[0] = TaskList.taskTemplates[ninja.getTaskId()].getMenuByIndex(ninja.getTaskIndex());
                }
            } else if (idnpc == 19 || idnpc == 22 || idnpc == 25) {
                m = new Message(40);
                if (idnpc == 19) {
                    m.writer().writeUTF("Không nhận EXP");
                    m.writer().writeUTF("Đổi mật khẩu");
                    m.writer().writeUTF("Admin gửi đồ");
                }
                if (idnpc == 22) {
                    p.nj.menuType = 1;
                    m.writer().writeUTF("Điểm danh gia tộc");
                }
                if (idnpc == 25) {
                    m.writer().writeUTF("Nhận thưởng thăng cấp");
                }
                m.writer().flush();
                p.session.sendMessage(m);
                m.cleanup();
                return;
            }
            if (ninja.getTaskId() == 23 && idnpc == 23 && ninja.getTaskIndex() == 1) {
                captions = new String[1];
                captions[0] = "Nhận chìa khoá";
            } else if (ninja.getTaskId() == 32 && idnpc == 20 && ninja.getTaskIndex() == 1) {
                captions = new String[1];
                captions[0] = "Nhận cần câu";
            }
            Service.openUIMenu(ninja, captions);
        }
    }

    @SneakyThrows
    public void selectMenuNpc(final User p, final Message m) throws IOException {

        val idNpc = (short) m.reader().readByte();
        val index = m.reader().readByte();
        if (idNpc == 0 && p.nj.getTaskId() != 13) {
            if (index == 0) {
                server.menu.sendWrite(p.nj, (short) 3, "Nhập số tiền cược");
            } else if (index == 1) {
                if (p.nj.getBattle() != null) {
                    p.nj.getBattle().setState(Battle.BATTLE_END_STATE);
                }
            }
        } else if (idNpc == Manager.ID_EVENT_NPC) {
            //  0: nhận lượng, 1: tắt exp, 2: bật up exp, 3: nhận thưởng level 70, 4: nhận thưởng level 90, 5: nhận thưởng lv 130
            short featureCode = Manager.ID_FEATURES[index];
//            switch (featureCode) {
//                case 1: {
//                    p.nj.get().exptype = 0;
//                    break;
//                }
//                case 2: {
//                    p.nj.get().exptype = 1;
//                    break;
//                }
//                case 3: {
//                    if (p.luong >= 10_000) {
//
//                        synchronized (p.nj){
//                            p.nj.maxluggage = 120;
//                        }
//
//                        p.upluongMessage(-10_000);
//                    } else {
//                        p.sendYellowMessage("Ta cũng cần ăn cơm đem 10.000 lượng đến đây ta thông hành trang cho");
//                    }
//                    break;
//                }
//                default:
//                    p.nj.getPlace().chatNPC(p, idNpc, "Ta đứng đây từ " + (util.nextInt(0, 1) == 1 ? "chiều" : "trưa"));
//            }
        } else if (idNpc == 33 && server.manager.EVENT != 0) {
            if (EventItem.entrys.length == 0) return;
            if (index < EventItem.entrys.length) {
                EventItem entry = EventItem.entrys[index];
                if (entry != null) {
                    lamSuKien(p, entry);
                }
            } else if (index == EventItem.entrys.length) {
                String huongDan = "";
                for (EventItem entry : EventItem.entrys) {
                    String s = "";
                    Recipe[] inputs = entry.getInputs();
                    for (int i = 0, inputsLength = inputs.length; i < inputsLength; i++) {
                        Recipe input = inputs[i];
                        val data = input.getItem().getData();
                        s += input.getCount() + " " + data.name;
                        if (inputsLength != inputs.length - 1) {
                            s += ", ";
                        }

                    }
                    huongDan += "- Để làm " + entry.getOutput().getItem().getData().name + " cần " + s;
                    if (entry.getIdRequired() != -1) {
                        huongDan += " " + ItemData.ItemDataId(entry.getIdRequired()).name;
                    }
                    if (entry.getCoin() > 0) {
                        huongDan += " " + entry.getCoin() + " xu";
                    }

                    if (entry.getCoinLock() > 0) {
                        huongDan += " " + entry.getCoinLock() + " yên";
                    }

                    if (entry.getGold() > 0) {
                        huongDan += " " + entry.getGold() + " lượng";
                    }
                    huongDan += "\n\n";
                }

                Service.sendThongBao(p.nj, huongDan);
            } else {
                 this.server.manager.sendTB(p, "Top Sự kiện", BXHManager.getStringBXH(5));
            }


        } else if (idNpc == 32 && p.nj.getPlace().map.isGtcMap()) {
            if (index == 0) {
                // Tong ket
                Service.sendBattleResult(p.nj, p.nj.getClanBattle());
            } else if (index == 1) {

                // Roi khoi day
                p.nj.changeTypePkNormal(Constants.PK_NORMAL);
                p.nj.getPlace().gotoHaruna(p);
            }
        } else {
            TaskHandle.getTask(p.nj, (byte) idNpc, index, (byte) -1);
        }
        m.cleanup();
    }


    public static void lamSuKien(User p, EventItem entry) throws IOException {
        boolean enough = true;
        boolean enough2 = false;
        for (Recipe input : entry.getInputs()) {
            int id = input.getId();
            enough = p.nj.enoughItemId(id, input.getCount());
            if (!enough) {
                p.nj.getPlace().chatNPC(p, (short) 33, "Con không đủ " + input.getItemData().name + " để làm sự kiện");
                break;
            }
            if (entry.getIdRequired() != -1) {
                enough2 = p.nj.enoughItemId(entry.getIdRequired(), 1);
                if (!enough2) {
                    p.nj.getPlace().chatNPC(p, (short) 33, "Con không đủ " + ItemData.ItemDataId(entry.getIdRequired()).name + " để làm sự kiện");
                    break;
                }
            } else {
                enough2 = true;
            }
        }
        if (enough && enough2 && p.nj.xu >= entry.getCoin() && p.nj.yen >= entry.getCoinLock() && p.nj.luong >= entry.getGold()) {
            for (Recipe input : entry.getInputs()) {
                p.nj.removeItemBags(input.getId(), input.getCount());
            }
            if (entry.getIdRequired() != -1) {
                p.nj.removeItemBags(entry.getIdRequired(), 1);
            }
            p.nj.addItemBag(true, entry.getOutput().getItem());
            p.nj.upxuMessage(-entry.getCoin());
            p.nj.upyenMessage(-entry.getCoinLock());
            p.upluongMessage(-entry.getGold());
        }
    }

    private boolean receiverSingleItem(User p, short idItem, int count) {
        if (!p.nj.checkHanhTrang(count)) {
            p.sendYellowMessage(MSG_HANH_TRANG);
            return true;
        }
        for (int i = 0; i < count; i++) {
            p.nj.addItemBag(false, ItemData.itemDefault(idItem));
        }
        return false;
    }

    private boolean nhanQua(User p, short[] idThuong) {
        if (p.nj.getAvailableBag() == 0) {
            p.sendYellowMessage("Hành trang phải đủ " + idThuong.length + " ô để nhận vật phẩm");
            return true;
        }
        for (short i : idThuong) {
            if (i == 12) {
                val quantity = util.nextInt(100_000_000, 150_000_000);
                p.nj.upyen(quantity);
            } else {
                Item item = ItemData.itemDefault(i);
                p.nj.addItemBag(false, item);
            }
        }
        return false;
    }

    @SneakyThrows
    public static void createMenu(int idNpc, String[] menu, String npcChat, User p) {
        val m = new Message(39);
        m.writer().writeShort(idNpc);
        m.writer().writeUTF(npcChat);
        m.writer().writeByte(menu.length);
        for (String s : menu) {
            m.writer().writeUTF(s);
        }

        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public static void doMenuArray(final User p, final String[] menu) throws IOException {
        final Message m = new Message(63);
        for (byte i = 0; i < menu.length; ++i) {
            m.writer().writeUTF(menu[i]);
        }
        m.writer().flush();
        p.sendMessage(m);
        m.cleanup();
    }

    public void sendWrite(final Ninja p, final short type, final String title) {
        try {
            final Message m = new Message(92);
            m.writer().writeUTF(title);
            m.writer().writeShort(type);
            m.writer().flush();
            p.sendMessage(m);
            m.cleanup();
        } catch (IOException ex) {
        }
    }

}
