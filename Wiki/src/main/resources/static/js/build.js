const specialCardColors = {
  "푸른 눈의 백룡": "#e6f5ff",
  "블랙 매지션": "#91278F",
  "암흑 기사 가이아": "#0067A3",
  "커스 오브 드래곤": "#D6CB63",
  "빛의 봉인검": "#B3D0B0",
  "봉인된 자의 오른쪽 팔": "#D97C2B",
  "봉인된 자의 왼쪽 팔": "#D97C2B",
  "봉인된 자의 오른쪽 다리": "#D97C2B",
  "봉인된 자의 왼쪽 다리": "#D97C2B",
  "봉인된 엑조디아": "#D97C2B",
  "붉은 눈의 흑룡": "#1F2032",
  "데몬 소환": "#DCDCDC",
  "해피 레이디 세자매": "#DB9700",
  "만화경 －화려한 분신－": "#108E7B",
  "장미에 사는 악령": "#8F4F9A",
  "블랙 데몬즈 드래곤": "#8F4F9A",
  "마음의 변화": "#108E7B",
  "죽은 자에게 흔드는 손": "#108E7B",
  "유쾌한 장의사": "#108E7B",
  "그레이트 모스": "#BB6A3F",
  "사우전드 드래곤": "#8F4F9A",
  "신의 심판": "#AE2A79",
  "승천의 뿔피리": "#AE2A79",
  "도적의 7가지 도구": "#AE2A79",
  "매직 재머": "#AE2A79",
  "속사포 드래곤": "#BB6A3F",
  "두 머리의 썬더 드래곤": "#8F4F9A",
  "오른손엔 방패 왼손엔 검": "#108E7B",
  "성스러운 방어막 거울의 힘": "#AE2A79",
  "트라이혼 드래곤": "#132436",
  "죽은 자의 소생": "#108E7B",
  "용기사 가이아": "#0067A3",
  "푸른 눈의 툰 드래곤": "#e6f5ff",
  "데몬의 도끼": "#108E7B",
  "육망성의 저주": "#AE2A79",
  새크리파이스: "#4B72C4",
  강탈: "#108E7B",
  "짓궃은 쌍둥이 악마": "#108E7B",
  "강인한 파수병": "#108E7B",
  싸이크론: "#108E7B",
  거대화: "#108E7B",
  "툰 인어": "#BB6A3F",
  "툰 데몬": "#DCDCDC",
};

const saveDeckData = sessionStorage.getItem("myDeckList");
let myDeckList = [];
let deckCount = 0;
let deckduplicate = {};

function jumpToPage() {
  const input = document.getElementById("jumpPageVal");
  if (!input) return;

  let targetPage = parseInt(input.value);
  const maxPage = parseInt(input.getAttribute("max"));

  if (isNaN(targetPage) || targetPage < 1 || targetPage > maxPage) {
    alert("1부터 " + maxPage + " 사이의 페이지를 입력해주세요");
    return;
  }

  document.getElementById("pageInput").value = targetPage - 1;
  document.getElementById("searchForm").submit();
}

function toggleSort(type) {
  const sortInput = document.getElementById("sortInput");
  const currentSort = document.getElementById("sortInput").value;
  let newSort = "";

  if (type === "atk") {
    newSort = currentSort === "atk_desc" ? "atk_asc" : "atk_desc";
  } else if (type === "def") {
    newSort = currentSort === "def_desc" ? "def_asc" : "def_desc";
  }

  document.getElementById("sortInput").value = newSort;
  document.getElementById("searchForm").submit();
}

window.onload = function () {
  if (
    typeof loadedDeckData !== "undefined" &&
    loadedDeckData !== null &&
    loadedDeckData.length > 0
  ) {
    myDeckList = [];
    deckCount = 0;
    deckduplicate = {};
    const deckListEl = document.getElementById("deck-list");
    if (deckListEl) deckListEl.innerHTML = "";

    loadedDeckData.forEach((card) => {
      addToDeck(
        card.cardname,
        card.id,
        false,
        card.atk,
        card.def,
        card.cardNumber,
        card.cardType || card.type,
      );
    });
    saveToStorage();
  } else if (saveDeckData) {
    const parsedDeck = JSON.parse(saveDeckData);
    parsedDeck.forEach((card) => {
      addToDeck(
        card.name,
        card.id,
        false,
        card.atk,
        card.def,
        card.cardNumber,
        card.cardType,
      );
    });
  }
  paintSpecialCards();
};
function paintSpecialCards() {
  document.querySelectorAll(".card-item").forEach((card) => {
    const name = card.dataset.name;
    if (specialCardColors[name]) {
      card.style.backgroundColor = specialCardColors[name];
      card.classList.add("holo-card");
    }
  });
}
function handleCardClick(element) {
  addToDeck(
    element.dataset.name,
    element.dataset.id,
    true,
    element.dataset.atk,
    element.dataset.def,
    element.dataset.cardNumber,
    element.dataset.cardType,
  );
}
function addToDeck(
  cardName,
  cardId,
  saveMode = true,
  cardAtk = "?",
  cardDef = "?",
  cardNumber = null,
  cardType = "Normal",
) {
  const isExtra = isExtraDeckCard(cardType);
  const mainDeckCount =
    document.getElementById("deck-list").children.length -
    (document.getElementById("empty-msg") ? 1 : 0);
  const extraDeckCount =
    document.getElementById("extra-deck-list").children.length;
  if (isExtra && extraDeckCount >= 15) {
    if (saveMode) alert("엑스트라 덱은 15장 까지만 넣을 수 있습니다.");
    return;
  }
  if (!isExtra && mainDeckCount >= 60) {
    if (saveMode) alert("메인 덱은 60장까지만 넣을 수 있습니다.");
    return;
  }
  let currentCardCount = deckduplicate[cardName] || 0;
  if (currentCardCount >= 3) {
    if (saveMode) alert("같은 카드는 3장까지만 넣을 수 있습니다");
    return;
  }
  if (!isExtra) {
    const emptyMsg = document.getElementById("empty-msg");
    if (emptyMsg) emptyMsg.style.display = "none";
  }
  const newDiv = document.createElement("div");
  newDiv.className = "deck-item";
  newDiv.dataset.id = cardId;
  newDiv.dataset.name = cardName;
  newDiv.dataset.cardNumber = cardNumber;
  newDiv.dataset.cardType = cardType;
  if (!cardAtk || cardAtk === "?" || cardAtk === "null") {
    newDiv.innerText = cardName;
  } else {
    newDiv.innerText = `${cardName}(공격력: ${cardAtk} / 수비력: ${cardDef})`;
  }

  if (specialCardColors[cardName]) {
    newDiv.style.backgroundColor = specialCardColors[cardName];
    newDiv.classList.add("holo-card");
    newDiv.style.color = "#333";
    newDiv.style.border = "1px solid gold";
  }

  newDiv.onclick = function () {
    this.remove();

    deckduplicate[cardName]--;
    const indexToRemove = myDeckList.findIndex(
      (item) => item.name === cardName,
    );
    if (indexToRemove > -1) {
      myDeckList.splice(indexToRemove, 1);
    }
    updateDeckCounts();
    saveToStorage();
  };

  if (isExtra) {
    document.getElementById("extra-deck-list").appendChild(newDiv);
  } else {
    document.getElementById("deck-list").appendChild(newDiv);
  }

  deckduplicate[cardName] = currentCardCount + 1;

  const cardObj = {
    name: cardName,
    id: cardId,
    atk: cardAtk,
    def: cardDef,
    cardNumber: cardNumber,
    cardType: cardType,
  };

  if (saveMode) {
    myDeckList.push(cardObj);
    saveToStorage();
  } else {
    myDeckList.push(cardObj);
  }

  updateDeckCounts();
}

function updateDeckCounts() {
  const emptyMsg = document.getElementById("empty-msg");

  const mainCount = document.querySelectorAll("#deck-list .deck-item").length;
  const extraCount = document.querySelectorAll(
    "#extra-deck-list .deck-item",
  ).length;

  if (mainCount === 0 && emptyMsg) {
    emptyMsg.style.display = "block";
  } else if (emptyMsg) {
    emptyMsg.style.display = "none";
  }

  const mainCountEl = document.getElementById("main-deck-count");
  const extraCountEl = document.getElementById("extra-deck-count");

  if (mainCountEl) mainCountEl.innerText = `(${mainCount})`;
  if (extraCountEl) extraCountEl.innerText = `(${extraCount})`;
}

function resetDeck() {
  if (!confirm("덱을 초기화 하시겠습니까?")) {
    return;
  }
  const deckList = document.getElementById("deck-list");

  deckList.innerHTML = `<div style="color: #ccc; text-align: center; margin-top: 50px" id="empty-msg">
          카드를 선택해주세요
          </div>`;

  const extraDeckList = document.getElementById("extra-deck-list");
  if (extraDeckList) {
    extraDeckList.innerHTML = "";
  }
  deckCount = 0;
  deckduplicate = {};
  myDeckList = [];
  sessionStorage.removeItem("myDeckList");

  const mainCountEl = document.getElementById("main-deck-count");
  const extraCountEl = document.getElementById("extra-deck-count");

  if (mainCountEl) mainCountEl.innerText = "(0)";
  if (extraCountEl) extraCountEl.innerText = "(0)";
}
function saveToStorage() {
  sessionStorage.setItem("myDeckList", JSON.stringify(myDeckList));
}

function saveDeck() {
  const mainDeckCount = document.querySelectorAll(
    "#deck-list .deck-item",
  ).length;
  const actualDataCount = myDeckList.filter(
    (card) => !isExtraDeckCard(card.cardType),
  ).length;
  if (myDeckList.length < 40) {
    alert(`메인 덱은 최소 40장 이상이어야 합니다. \n(현재: ${mainDeckCount})`);
    return;
  }

  const cardIds = myDeckList
    .map((card) => parseInt(card.id))
    .filter((id) => !isNaN(id));

  const deckName = prompt("저장할 덱의 이름을 입력해주세요:", "덱");
  if (!deckName || deckName.trim() === "") return;

  fetch("/Wiki/deck/save", {
    method: "POST",
    headers: {
      "Content-type": "application/json",
    },
    body: JSON.stringify({
      name: deckName,
      cardIds: cardIds,
    }),
  })
    .then(async (response) => {
      if (response.ok) {
        alert("덱 저장에 성공했습니다.");
      } else {
        alert("실패: " + (await response.text()));
      }
    })
    .catch((err) => {
      console.error("Error:", err);
      alert("오류가 발생했습니다.");
    });
}
function showDeckInGrid() {
  const gridArea = document.getElementById("searchResultArea");
  const mainItems = Array.from(
    document.querySelectorAll("#deck-list .deck-item"),
  );
  const extraItems = Array.from(
    document.querySelectorAll("#extra-deck-list .deck-item"),
  );

  const allItems = mainItems.concat(extraItems);

  gridArea.innerHTML = "";

  if (allItems.length === 0) {
    gridArea.innerHTML =
      '<p style="grid-column: 1 / -1; text-align:center; margin-top:50px;">아직 덱에 카드가 없습니다</p>';
    return;
  }

  if (mainItems.length > 0) {
    gridArea.insertAdjacentHTML(
      "beforeend",
      '<h3 style="grid-column: 1 / -1; width:100%; margin:20px 0 10px; border-bottom:1px solid #ccc;">메인 덱</h3>',
    );
    renderCardsToGrid(mainItems, gridArea);
  }

  if (extraItems.length > 0) {
    gridArea.insertAdjacentHTML(
      "beforeend",
      '<h3 style=" grid-column: 1 / -1; width:100%; margin:20px 0 10px; border-bottom:1px solid #ccc;">엑스트라 덱</h3>',
    );
    renderCardsToGrid(extraItems, gridArea);
  }

  paintSpecialCards();
}

function renderCardsToGrid(items, container) {
  items.forEach((item) => {
    const cardName = item.dataset.name;
    const cardNumber = item.dataset.cardNumber;

    if (!cardNumber) return;
    const cardHtml = `
    <div class="card-item" data-name="${cardName}">
      <div class="card-frame holo-effect">
        <img src="https://images.ygoprodeck.com/images/cards/${cardNumber}.jpg"
          class="card-img"
          onerror="this.onerror=null; this.src='/img/card_back.png';"
          style="width: 100%; border-radius: 5px;">
          <div class="holo-overlay"></div>
      </div>
      <div class="card-name" style="text-align: center; margin-top: 5px; font-weight: bold; color:#333;">${cardName}</div>
    </div>
        `;
    container.insertAdjacentHTML("beforeend", cardHtml);
  });
}

function isExtraDeckCard(cardType) {
  if (!cardType) return false;

  const type = cardType.toLowerCase();

  return (
    type.includes("fusion") ||
    type.includes("synchro") ||
    type.includes("xyz") ||
    type.includes("link")
  );
}
