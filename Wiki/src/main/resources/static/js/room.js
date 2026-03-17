let currentRoomId = null;
let isMyTurn = false;
let currentRoomData = null;

let isFusionMode = false;
let fusionTargetId = null;
let selectedMaterials = [];
let summonableFusions = [];
let isDiscardMode = false;

let isTargetingMode = false;
let activeSpellId = null;

$(document).ready(function () {
  const urlParams = new URLSearchParams(window.location.search);
  currentRoomId = urlParams.get("roomId");

  if (!currentRoomId) {
    alert("잘못된 접근입니다");
    location.href = "/Wiki/game/select";
    return;
  }
  console.log("게임방 입장 완료 Room ID: ", currentRoomId);
  loadGameInfo();
});
function loadGameInfo() {
  $.ajax({
    url: "/Wiki/game/info?roomId=" + currentRoomId,
    type: "GET",
    success: function (room) {
      console.log("현재 방 상태:", room);
      updateBoard(room);
    },
    error: function (xhr) {
      alert("게임 정보를 불러오지 못했습니다.");
      location.href = "/Wiki/game/select";
    },
  });
}
function updateBoard(room) {
  currentRoomData = room;
  const user = room.user;
  const cpu = room.cpu;

  $("#myHp").text(user.hp);
  $("#myDeckCount").text(user.deck.length);
  $("#myExtraDeckCount").text(user.extradeck.length);
  $("#myCemeteryCount").text(user.cemetery.length);

  $("#cpuHp").text(cpu.hp);
  $("#cpuDeckCount").text(cpu.deck.length);
  $("#cpuCemeteryCount").text(cpu.cemetery.length);

  $("#turnOwnerDisplay").text(
    "Turn: " + (room.currentTurnOwner ? room.currentTurnOwner : "준비 중"),
  );
  $("#levelLimitDisplay").text(room.levelLimit);

  const $logBox = $("#gameLog");
  $logBox.html(room.logs.join("<br>"));
  $logBox.scrollTop($logBox[0].scrollHeight);

  isMyTurn = room.currentTurnOwner === "USER";
  if (isMyTurn && room.status === "PLAYING") {
    $("#btnEndTurn").show();
  } else {
    $("#btnEndTurn").hide();
  }

  checkSummonableFusions();

  renderMyHand(user.hand);
  renderCpuHand(cpu.hand.length);
  renderField(user, cpu);

  if (room.status === "RPS") {
    promptRps();
  }

  if (room.status === "USER_WIN") {
    setTimeout(() => {
      alert("듀얼에서 승리하셨습니다!");
      location.href = "/Wiki/game/select";
    }, 500);
    return;
  } else if (room.status === "CPU_WIN") {
    setTimeout(() => {
      alert("듀얼에서 패배하였습니다...");
      location.href = "/Wiki/game/select";
    }, 500);
    return;
  }

  isMyTurn = room.currentTurnOwner === "USER";
  if (isMyTurn && room.status === "PLAYING") {
    $("#btnEndTurn").show();
  } else {
    $("#btnEndTurn").hide();
  }
}

function renderField(user, cpu) {
  const $myMonZone = $("#myMonsterZone");
  $myMonZone.empty();
  const myBuff = calculateBuff(user.hand, user.monsterZone);
  const cpuBuff = calculateBuff(cpu.hand, cpu.monsterZone);
  if (user.monsterZone) {
    const card = user.monsterZone;
    const imgUrl = `https://images.ygoprodeck.com/images/cards/${card.originalId}.jpg`;
    let myEquipAtk = 0;
    let myEquipDef = 0;
    let equipBadgeHtml = "";
    let equipTooltipHtml = "";
    if (card.equippedCards && card.equippedCards.length > 0) {
      equipBadgeHtml = `<div class="equip-badge">E:${card.equippedCards.length}</div>`;

      let equipList = card.equippedCards
        .map((eq) => `<li>${eq.name} (공+${eq.atk} / 수+${eq.def})</li>`)
        .join("");

      equipTooltipHtml = `
          <div class="equip-tooltip">
              <div style="color: #ffcc00; margin-bottom: 3px; border-bottom: 1px solid #555;">[장착된 카드]</div>
              <ul style="margin: 0; padding-left: 15px;">${equipList}</ul>
          </div>
      `;

      card.equippedCards.forEach((equip) => {
        myEquipAtk += equip.atk;
        myEquipDef += equip.def;
      });
    }
    let myFieldAtk = 0;
    let myFieldDef = 0;
    if (user.fieldZone) {
      const fBuffs = calculateFieldBuff(user.fieldZone, card);
      myFieldAtk += fBuffs.atk;
      myFieldDef += fBuffs.def;
    }

    if (cpu.fieldZone) {
      const fBuffs = calculateFieldBuff(cpu.fieldZone, card);
      myFieldAtk += fBuffs.atk;
      myFieldDef += fBuffs.def;
    }
    const totalAtkBuff = myBuff + myEquipAtk + myFieldAtk;
    const totalDefBuff = myBuff + myEquipDef + myFieldDef;

    const atkText =
      totalAtkBuff > 0
        ? `<span style="color: #00ff00;">(+${totalAtkBuff})</span>`
        : totalAtkBuff < 0
          ? `<span style="color: #ff0000;">(${totalAtkBuff})</span>`
          : "";
    const defText =
      totalDefBuff > 0
        ? `<span style="color: #00ff00;">(+${totalDefBuff})</span>`
        : totalDefBuff < 0
          ? `<span style="color: #ff0000;">(${totalDefBuff})</span>`
          : "";
    $myMonZone.html(
      `<div class="card-wrapper">
          ${equipBadgeHtml}
          ${equipTooltipHtml}
          <div class="card" onclick="clickMyMonster('${card.uniqueId}','${card.name}')"
               style="background-image: url('${imgUrl}'); background-size: cover; background-position: center; border: 2px solid #0088ff; display: flex; align-items: flex-end; padding: 0; min-width: 100px; height: 140px; border-radius: 5px; cursor: pointer; overflow: hidden;">
              <div style="background: rgba(0, 0, 0, 0.85); width: 100%; padding: 4px; box-sizing: border-box; color: white;">
                  <div style="font-size: 0.65rem; color: #ffcc00;">[Lv.${card.level}]</div>
                  <div style="font-weight: bold; margin: 2px 0; font-size: 0.75rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${card.name}</div>
                  <div style="font-size: 0.65rem; color: #aaa;">공: ${card.atk} ${atkText} / 수: ${card.def} ${defText}</div>
              </div>
          </div>
      </div>`,
    );
  } else {
    $myMonZone.html("몬스터 존");
  }

  const $cpuMonZone = $("#cpuMonsterZone");
  $cpuMonZone.empty();
  if (cpu.monsterZone) {
    const card = cpu.monsterZone;
    const imgUrl = `https://images.ygoprodeck.com/images/cards/${card.originalId}.jpg`;

    let cpuEquipAtk = 0;
    let cpuEquipDef = 0;

    let equipBadgeHtml = "";
    let equipTooltipHtml = "";
    if (card.equippedCards && card.equippedCards.length > 0) {
      equipBadgeHtml = `<div class="equip-badge">E:${card.equippedCards.length}</div>`;
      let equipList = card.equippedCards
        .map((eq) => `<li>${eq.name} (공+${eq.atk} / 수+${eq.def})</li>`)
        .join("");

      equipTooltipHtml = `<div class="equip-tooltip">
              <div style="color: #ffcc00; margin-bottom: 3px; border-bottom: 1px solid #555;">[장착된 카드]</div>
              <ul style="margin: 0; padding-left: 15px;">${equipList}</ul>
          </div>`;
      card.equippedCards.forEach((equip) => {
        cpuEquipAtk += equip.atk;
        cpuEquipDef += equip.def;
      });
    }
    let cpuFieldAtk = 0;
    let cpuFieldDef = 0;
    if (user.fieldZone) {
      const fBuffs = calculateFieldBuff(user.fieldZone, card);
      cpuFieldAtk += fBuffs.atk;
      cpuFieldDef += fBuffs.def;
    }

    if (cpu.fieldZone) {
      const fBuffs = calculateFieldBuff(cpu.fieldZone, card);
      cpuFieldAtk += fBuffs.atk;
      cpuFieldDef += fBuffs.def;
    }

    const totalCpuAtkBuff = cpuBuff + cpuEquipAtk + cpuFieldAtk;
    const totalCpuDefBuff = cpuBuff + cpuEquipDef + cpuFieldDef;

    const cpuAtkText =
      totalCpuAtkBuff > 0
        ? `<span style="color: #00ff00;">(+${totalCpuAtkBuff})</span>`
        : totalCpuAtkBuff < 0
          ? `<span style="color: #ff0000;">(${totalCpuAtkBuff})</span>`
          : "";
    const cpuDefText =
      totalCpuDefBuff > 0
        ? `<span style="color: #00ff00;">(+${totalCpuDefBuff})</span>`
        : totalCpuDefBuff < 0
          ? `<span style="color: #ff0000;">(${totalCpuDefBuff})</span>`
          : "";
    $cpuMonZone.html(
      `<div class="card-wrapper">
          ${equipBadgeHtml}
          ${equipTooltipHtml}
          <div class="card" onclick="clickCpuMonster('${card.uniqueId}', '${card.name}')"
               style="background-image: url('${imgUrl}'); background-size: cover; background-position: center; border: 2px solid #ff0000; display: flex; align-items: flex-end; padding: 0; min-width: 100px; height: 140px; border-radius: 5px; cursor: pointer; overflow: hidden;">
              <div style="background: rgba(0, 0, 0, 0.85); width: 100%; padding: 4px; box-sizing: border-box; color: white;">
                  <div style="font-size: 0.65rem; color: #ffcc00;">[Lv.${card.level}]</div>
                  <div style="font-weight: bold; margin: 2px 0; font-size: 0.75rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${card.name}</div>
                  <div style="font-size: 0.65rem; color: #aaa;">공: ${card.atk} ${cpuAtkText} / 수: ${card.def} ${cpuDefText}</div>
              </div>
          </div>
      </div>`,
    );
  } else {
    $cpuMonZone.html("몬스터 존");
  }

  const $myFieldSpellZone = $("#myFieldSpellZone");
  const $cpuFieldSpellZone = $("#cpuFieldSpellZone");

  $myFieldSpellZone.empty();
  $cpuFieldSpellZone.empty();

  if (user.fieldZone) {
    const fSpell = user.fieldZone;
    const imgUrl = `https://images.ygoprodeck.com/images/cards/${fSpell.originalId}.jpg`;
    console.log(`받아온 카드넘버 : ${fSpell.originalId}`);
    const fieldHtml = `
      <div class="card-wrapper">
          <div class="equip-tooltip" style="bottom: 110%;">
              <div style="color: #00ff00; margin-bottom: 3px;">[현재 필드 마법]</div>
              <div>${fSpell.name} (공+${fSpell.atk} / 수+${fSpell.def})</div>
          </div>
          <div class="card" style="background-image: url('${imgUrl}'); background-size: cover; background-position: center; border: 2px solid #00ff00; min-width: 100px; height: 140px; border-radius: 5px; box-shadow: 0 0 15px;">
          </div>
      </div>
    `;
    $myFieldSpellZone.html(fieldHtml);
  } else {
    $myFieldSpellZone.html("필드 마법");
  }

  if (cpu.fieldZone) {
    const fSpell = cpu.fieldZone;
    const imgUrl = `https://images.ygoprodeck.com/images/cards/${fSpell.originalId}.jpg`;

    const cpuFieldHtml = `<div class="card-wrapper">
          <div class="equip-tooltip" style="bottom: 110%;">
              <div style="color: #ffcc00; margin-bottom: 3px;">[상대 필드 마법]</div>
              <div>${fSpell.name} (공+${fSpell.atk} / 수+${fSpell.def})</div>
          </div>
          <div class="card" style="background-image: url('${imgUrl}'); background-size: cover; background-position: center; border: 2px solid #ff0000; min-width: 100px; height: 140px; border-radius: 5px; box-shadow: 0 0 15px">
          </div>
      </div>`;

    $cpuFieldSpellZone.html(cpuFieldHtml);
  } else {
    $cpuFieldSpellZone.html("필드 마법");
  }
}

function requestSummon(uniqueId, cardName, cardType, detailType) {
  if (!isMyTurn) {
    showToast("내 턴이 아닙니다.");
    return;
  }

  if (isDiscardMode) {
    $.ajax({
      url: "/Wiki/game/discard",
      type: "POST",
      contentType: "application/json",
      data: JSON.stringify({ roomId: currentRoomId, cardUniqueId: uniqueId }),
      success: function (room) {
        const handCount = room.user.hand.length;

        if (handCount > 7) {
          showToast(
            `[${cardName}] 버림. 앞으로 ${handCount - 7}장 더 버려주세요`,
          );
          setTimeout(() => {
            $("#myHand .card").addClass("fusion-glow");
          }, 100);
        } else {
          showToast("패가 7장이 되어 턴종료가 가능합니다.");
          isDiscardMode = false;
          $(".fusion-glow").removeClass("fusion-glow");
        }
        updateBoard(room);
      },
      error: function (xhr) {
        alert("버리기 실패: " + xhr.responseText);
      },
    });
    return;
  }

  if (
    cardType &&
    (cardType.includes("Magic") ||
      cardType.includes("Spell") ||
      cardType.includes("마법"))
  ) {
    if (detailType && detailType.toLowerCase().includes("equip")) {
      isTargetingMode = true;
      activeSpellId = uniqueId;

      $("#myMonsterZone .card").addClass("fusion-glow");
      $("#cpuMonsterZone .card").addClass("fusion-glow");

      showToast(`[${cardName}] 선택됨. 장착할 몬스터를 선택해주세요`);
      return;
    } else if (detailType && detailType.toLowerCase().includes("field")) {
      if (!confirm(`필드마법 [${cardName}]를 발동하시겠습니까?`)) {
        return;
      }
      $.ajax({
        url: "/Wiki/game/field-spell",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify({
          roomId: currentRoomId,
          spellUniqueId: uniqueId,
        }),
        success: function (room) {
          updateBoard(room);
          showToast(`[${cardName}] 발동 성공`);
        },
        error: function (xhr) {
          showToast("발동 실패: " + xhr.responseText);
        },
      });
      return;
    } else {
      showToast("불가");
      return;
    }
  }

  if (isFusionMode) {
    selectForFusion(uniqueId, cardName);
    return;
  }

  if (currentRoomData.user.monsterZone) {
    if (
      !confirm(
        "필드에 이미 몬스터가 있습니다. 기존 몬스터를 묘지로 보내고 소환하시겠습니까?",
      )
    ) {
      return;
    }
  }

  $.ajax({
    url: "/Wiki/game/summon",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify({ roomId: currentRoomId, cardUniqueId: uniqueId }),
    success: function (room) {
      updateBoard(room);
    },
    error: function (xhr) {
      alert(xhr.responseText);
    },
  });
}

function clickMyMonster(uniqueId, cardName) {
  if (!isMyTurn) return;

  if (isTargetingMode) {
    executeEquip(uniqueId, cardName);
    return;
  }

  if (isFusionMode) {
    selectForFusion(uniqueId, cardName);
    return;
  }

  $.ajax({
    url: "/Wiki/game/battle",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify({ roomId: currentRoomId }),
    success: function (room) {
      updateBoard(room);
    },
    error: function (xhr) {
      alert(xhr.responseText);
    },
  });
}

function renderMyHand(handList) {
  const $handZone = $("#myHand");
  $handZone.empty();

  handList.forEach((card) => {
    const imageUrl = `https://images.ygoprodeck.com/images/cards/${card.originalId}.jpg`;
    const cardHtml = `<div id="card-${card.uniqueId}" class="card" onclick="requestSummon('${card.uniqueId}', '${card.name}','${card.cardType}','${card.monsterType}')" 
             style="background-image: url('${imageUrl}'); background-size: cover; background-position: center; border: 2px solid #ffcc00; display: flex; align-items: flex-end; padding: 0; min-width: 100px; height: 140px; cursor: pointer; border-radius: 5px; overflow: hidden;">
            
            <div style="background: rgba(0, 0, 0, 0.85); width: 100%; padding: 4px; box-sizing: border-box; color: white;">
                <div style="font-size: 0.65rem; color: #ffcc00;">[Lv.${card.level}]</div>
                <div style="font-weight: bold; margin: 2px 0; font-size: 0.75rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${card.name}</div>
                <div style="font-size: 0.65rem; color: #aaa;">공: ${card.atk} / 수: ${card.def}</div>
            </div>
            
        </div>`;
    $handZone.append(cardHtml);
  });
}

function renderCpuHand(count) {
  const $handZone = $("#cpuHand");
  $handZone.empty();

  for (let i = 0; i < count; i++) {
    $handZone.append(
      `<div class="card card-back" style="background: url('/Wiki/img/card_back.png') center/cover; border: 2px solid #444;"></div>`,
    );
  }
}

function promptRps() {
  if ($("#rpsModal").length > 0) return;

  const rpsHtml = `
        <div id="rpsModal" style="position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.8); display:flex; flex-direction:column; justify-content:center; align-items:center; z-index:100;">
            <h2 style="color:#ffcc00; margin-bottom:20px;">선공을 결정합니다!</h2>
            <div>
                <button onclick="playRps('가위')" style="padding:15px 30px; font-size:20px; margin:10px; cursor:pointer;">가위 ✌️</button>
                <button onclick="playRps('바위')" style="padding:15px 30px; font-size:20px; margin:10px; cursor:pointer;">바위 ✊</button>
                <button onclick="playRps('보')" style="padding:15px 30px; font-size:20px; margin:10px; cursor:pointer;">보 🖐️</button>
            </div>
        </div>
    `;
  $("body").append(rpsHtml);
}

function playRps(choice) {
  $("#rpsModal").remove();

  $.ajax({
    url: "/Wiki/game/rps",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify({ roomId: currentRoomId, choice: choice }),
    success: function (room) {
      updateBoard(room);
    },
    error: function (xhr) {
      alert("오류 발생: " + xhr.responseText);
      loadGameInfo();
    },
  });
}

function endTurn() {
  if (!isMyTurn) {
    showToast("아직 내 턴이 아닙니다.");
    return;
  }

  if (currentRoomData.user.hand.length > 7) {
    showToast("패가 7장을 초과했습니다. 버릴 카드를 선택해주세요.");
    isDiscardMode = true;

    $("#myHand .card").addClass("fusion-glow");
    return;
  }

  $("#btnEndTurn").hide();

  $.ajax({
    url: "/Wiki/game/end-turn",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify({ roomId: currentRoomId }),
    success: function (room) {
      updateBoard(room);
    },
    error: function (xhr) {
      alert("턴 종료 오류: " + xhr.responseText);
      $("#btnEndTurn").show();
    },
  });
}
function openExtraDeck() {
  console.log("엑스트라 덱 버튼 클릭됨 현재 데이터:", currentRoomData);
  if (!currentRoomData) {
    alert("데이터를 아직 불러오지 못했습니다");
    return;
  }

  const extraDeck = currentRoomData.user.extradeck;
  if (extraDeck.length === 0) {
    alert("엑스트라 덱이 비었습니다");
    return;
  }

  let modalHtml = `<div id="extraDeckModal" style="position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.85); z-index:200; display:flex; flex-direction:column; align-items:center; justify-content:center;">
        <h2 style="color:#00eeff; text-shadow: 0 0 10px #0088ff; margin-bottom:20px;">엑스트라 덱</h2>
        <div style="display:flex; gap:15px; max-width:90%; overflow-x:auto; padding:30px; background:rgba(255,255,255,0.1); border-radius:15px; border:1px solid #555;">`;

  extraDeck.forEach((card) => {
    const imageUrl = `https://images.ygoprodeck.com/images/cards/${card.originalId}.jpg`;
    const glowClass = summonableFusions.includes(card.uniqueId)
      ? "fusion-glow"
      : "";
    modalHtml += `
        <div class="card ${glowClass}" onclick="selectExtraCard('${card.uniqueId}', '${card.name}')"
             style="background-image: url('${imageUrl}'); background-size: cover; background-position: center; border: 2px solid #00eeff; display: flex; align-items: flex-end; padding: 0; min-width: 120px; height: 168px;">
            <div style="background: rgba(0, 0, 0, 0.8); width: 100%; padding: 5px; box-sizing: border-box; color: white;">
                <div style="font-size: 0.6rem; color: #ffcc00;">[Lv.${card.level}]</div>
                <div style="font-weight: bold; margin: 3px 0; font-size: 0.75rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${card.name}</div>
                <div style="font-size: 0.65rem; color: #aaa;">공: ${card.atk} / 수: ${card.def}</div>
            </div>
        </div>
        `;
  });

  modalHtml += `
    </div>
        <button onclick="$('#extraDeckModal').remove()" style="margin-top:30px; padding:10px 30px; font-size:1.2rem; background:#333; color:#fff; border:1px solid #777; border-radius:5px; cursor:pointer;">닫기</button>
    </div>
  `;

  $("body").append(modalHtml);
}
function selectExtraCard(uniqueId, name) {
  $("#extraDeckModal").remove();
  isFusionMode = true;
  fusionTargetId = uniqueId;
  selectedMaterials = [];

  $.ajax({
    url: "/Wiki/game/fusion-materials",
    type: "GET",
    data: { roomId: currentRoomId, targetId: uniqueId },
    success: function (validIds) {
      $(".fusion-glow").removeClass("fusion-glow");

      if (validIds.length === 0) {
        alert(
          "현재 패와 필드에 [" + name + "]의 조건에 맞는 소재가 전혀 없습니다.",
        );
        cancelFusionMode();
        return;
      }
      validIds.forEach((id) => {
        $("#card-" + id).addClass("fusion-glow");
      });
      console.log("우클릭이나 ESC를 누르면 융합이 취소됩니다.");
    },
    error: function (xhr) {
      alert("소재 조회 실패:" + xhr.responseText);
      cancelFusionMode();
    },
  });
}
function surrenderGame() {
  if (confirm("항복하시겠습니까?")) {
    $.ajax({
      url: "/Wiki/game/surrender",
      type: "POST",
      contentType: "application/json",
      data: JSON.stringify({ roomId: currentRoomId }),
      success: function (room) {
        updateBoard(room);
      },
      error: function (xhr) {
        alert("오류발생: " + xhr.responseText);
      },
    });
  }
}
function selectForFusion(uniqueId, cardName) {
  if (selectedMaterials.some((mat) => mat.id === uniqueId)) {
    showToast("이미 선택한 카드입니다.");
    return;
  }

  selectedMaterials.push({ id: uniqueId, name: cardName });
  showToast(`소재선택: ${cardName} (${selectedMaterials.length}장 선택됨)`);

  if (selectedMaterials.length >= 2) {
    let confirmMsg =
      "선택한 몬스터들로 융합하시겠습니까? 취소를 누르시면 추가로 소재를 넣을 수 있습니다.";
    const fieldMonster = currentRoomData.user.monsterZone;
    if (fieldMonster) {
      const isFieldMonsterSelected = selectedMaterials.some(
        (mat) => mat.id === fieldMonster.uniqueId,
      );
      if (!isFieldMonsterSelected) {
        confirmMsg =
          "필드에 몬스터가 남아있습니다 \n 묘지로 보내고 소환하시겠습니까?";
      }
    }

    if (confirm(confirmMsg)) {
      const materialIds = selectedMaterials.map((mat) => mat.id);

      $.ajax({
        url: "/Wiki/game/fusion",
        type: "POST",
        contentType: "application/json",
        data: JSON.stringify({
          roomId: currentRoomId,
          targetId: fusionTargetId,
          materialIds: materialIds,
        }),
        success: function (room) {
          isFusionMode = false;
          selectedMaterials = [];
          cancelFusionMode();
          updateBoard(room);
        },
        error: function (xhr) {
          alert("융합실패: " + xhr.responseText);
          isFusionMode = false;
          selectedMaterials = [];
          cancelFusionMode();
        },
      });
    }
  }
}
function cancelFusionMode() {
  if (isFusionMode) {
    isFusionMode = false;
    fusionTargetId = null;
    selectedMaterials = [];

    $(".fusion-glow").removeClass("fusion-glow");

    console.log("융합소환이 취소되었습니다");
  }
}

function checkSummonableFusions() {
  if (!isMyTurn) {
    summonableFusions = [];
    return;
  }

  $.ajax({
    url: "/Wiki/game/summonable-fusions",
    type: "GET",
    data: { roomId: currentRoomId },
    success: function (ids) {
      summonableFusions = ids;
      if (ids.length > 0) {
        $("#myExtraDeckArea").addClass("fusion-glow");
      } else {
        $("#myExtraDeckArea").removeClass("fusion-glow");
      }
    },
  });
}
function calculateBuff(hand, fieldMonster) {
  if (!fieldMonster) return 0;
  if (!hand || hand.length === 0) return 0;

  const monsters = hand.filter((c) => c.cardType == "Monster");
  if (monsters.length === 0) return 0;

  const fieldRace = fieldMonster.race;
  const fieldElement = fieldMonster.element;

  let allSameRace = true;
  let allSameElement = true;

  for (let i = 0; i < monsters.length; i++) {
    if (!fieldRace || monsters[i].race !== fieldRace) {
      allSameRace = false;
    }
    if (!fieldElement || monsters[i].element !== fieldElement) {
      allSameElement = false;
    }
  }

  if (allSameRace && allSameElement) return 750;
  if (allSameRace || allSameElement) return 500;

  return 0;
}
function showToast(message) {
  $(".game-toast").remove();

  const toastHtml = `<div class="game-toast" style="position: fixed; top: 10%; left: 50%; transform: translateX(-50%); background: rgba(0, 0, 0, 0.85); color: #00eeff; padding: 12px 25px; border-radius: 8px; border: 1px solid #00eeff; box-shadow: 0 0 15px #00eeff; z-index: 9999; font-size: 1.1rem; font-weight: bold; pointer-events: none; transition: opacity 0.5s;">
      ${message}
    </div>`;

  $("body").append(toastHtml);

  setTimeout(() => {
    $(".game-toast").fadeOut(500, function () {
      $(this).remove();
    });
  }, 1500);
}
function cancelTargetingMode() {
  if (isTargetingMode) {
    isTargetingMode = false;
    activeSpellId = null;
    $(".fusion-glow").removeClass("fusion-glow");
    showToast("장착이 취소되었습니다.");
  }
}

$(document).on("keydown", function (e) {
  if (e.key === "Escape") {
    cancelFusionMode();
    cancelTargetingMode();
  }
});

$(document).on("contextmenu", function (e) {
  if (isFusionMode || isTargetingMode) {
    e.preventDefault();
    cancelFusionMode();
    cancelTargetingMode();
  }
});
function executeEquip(targetMonsterId, targetMonsterName) {
  $.ajax({
    url: "/Wiki/game/equip",
    type: "POST",
    contentType: "application/json",
    data: JSON.stringify({
      roomId: currentRoomId,
      spellUniqueId: activeSpellId,
      targetMonsterUniqueId: targetMonsterId,
    }),
    success: function (room) {
      cancelTargetingMode();
      updateBoard(room);
      showToast(`[${targetMonsterName}]에 장착 성공`);
    },
    error: function (xhr) {
      cancelTargetingMode();
      showToast("장착 실패: " + xhr.responseText);
    },
  });
}
function clickCpuMonster(uniqueId, cardName) {
  if (!isMyTurn) return;

  if (isTargetingMode) {
    executeEquip(uniqueId, cardName);
    return;
  }
}
function openCemetery(playerType) {
  if (!currentRoomData) {
    showToast("데이터를 아직 불러오지 못했습니다");
    return;
  }

  const cemeteryList =
    playerType === "USER"
      ? currentRoomData.user.cemetery
      : currentRoomData.cpu.cemetery;
  const ownerName = playerType === "USER" ? "내 묘지" : "상대방 묘지";

  if (!cemeteryList || cemeteryList.length === 0) {
    showToast(`${ownerName}가 텅 비었습니다`);
    return;
  }

  let modalHtml = `<div id="cemeteryModal" style="position:fixed; top:0; left:0; width:100%; height:100%; background:rgba(0,0,0,0.85); z-index:200; display:flex; flex-direction:column; align-items:center; justify-content:center;">
        <h2 style="color:#bbaaff; text-shadow: 0 0 10px #8844ff; margin-bottom:20px;">🪦 ${ownerName} (${cemeteryList.length}장) 🪦</h2>
        <div style="display:flex; gap:15px; max-width:90%; overflow-x:auto; padding:30px; background:rgba(255,255,255,0.1); border-radius:15px; border:1px solid #555;">`;

  cemeteryList.forEach((card) => {
    const imgUrl = `https://images.ygoprodeck.com/images/cards/${card.originalId}.jpg`;

    let statsHtml = "";
    if (card.cardType && card.cardType.includes("Monster")) {
      statsHtml = `<div style="font-size: 0.65rem; color: #ffcc00;">[Lv.${card.level}]</div>
                     <div style="font-size: 0.65rem; color: #aaa;">공: ${card.atk} / 수: ${card.def}</div>`;
    } else {
      statsHtml = `<div style="font-size: 0.65rem; color: #00eeff;">[${card.cardType}]</div>
                     <div style="font-size: 0.65rem; color: #aaa;">${card.monsterType || "마법/함정"}</div>`;
    }
    modalHtml += `<div class="card"
             style="background-image: url('${imgUrl}'); background-size: cover; background-position: center; border: 2px solid #555; display: flex; align-items: flex-end; padding: 0; min-width: 120px; height: 168px; filter: grayscale(50%);">
            <div style="background: rgba(0, 0, 0, 0.8); width: 100%; padding: 5px; box-sizing: border-box; color: white;">
                ${statsHtml}
                <div style="font-weight: bold; margin: 3px 0; font-size: 0.75rem; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${card.name}</div>
            </div>
        </div>`;
  });
  modalHtml += `</div>
        <button onclick="$('#cemeteryModal').remove()" style="margin-top:30px; padding:10px 30px; font-size:1.2rem; background:#333; color:#fff; border:1px solid #777; border-radius:5px; cursor:pointer;">닫기</button>
    </div>`;

  $("body").append(modalHtml);
}
function calculateFieldBuff(fieldSpell, monster) {
  if (!fieldSpell || !monster)
    return {
      atk: 0,
      def: 0,
    };
  console.log("현재 깔린 마법 데이터: ", fieldSpell);
  console.log("버프를 받을 몬스터 데이터: ", monster);

  let isMatch = false;
  const reqType = fieldSpell.reqType;
  const reqValue = fieldSpell.reqValue;

  console.log("필드마법의 발동 조건 - 타입: ", reqType, "/ 값: ", reqValue);

  if (!reqType || reqType.trim() === "") {
    console.log("결과: 조건없음으로 통과");
    isMatch = true;
  } else if (reqType.toUpperCase() === "RACE" && monster.race === reqValue) {
    console.log(`종족 일치 통과. 종족 : ${monster.race} `);
    isMatch = true;
  } else if (
    reqType.toUpperCase() === "ELEMENT" &&
    monster.element === reqValue
  ) {
    console.log(`속성 일치 통과: 속성 : ${monster.element}`);
    isMatch = true;
  } else if (
    reqType.toUpperCase() === "NOT_RACE" &&
    monster.race !== reqValue
  ) {
    isMatch = true;
  } else if (
    reqType.toUpperCase === "NOT_ELEMENT" &&
    monster.element !== reqValue
  ) {
    isMatch = true;
  } else {
    console.log("조건 불일치");
  }
  if (isMatch) {
    console.log(
      `적용될 버프 수치 공격럭: ${fieldSpell.atk}, 수비력: ${fieldSpell.def}`,
    );
    return { atk: fieldSpell.atk || 0, def: fieldSpell.def || 0 };
  }

  console.log("버프 적용 실패 0 반환");
  return { atk: 0, def: 0 };
}
