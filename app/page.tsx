"use client";

import { useState } from "react";

const copy = {
  pt: {
    nav: ["Produtos", "Para negócios", "Encomendar"],
    eyebrow: "Sabor local · Produção no Fundão",
    titleA: "Salgados feitos",
    titleB: "para partilhar.",
    intro: "Receitas com sabor português para a sua mesa, festa ou negócio. Produção local, encomenda simples e entrega a combinar.",
    cta: "Pedir catálogo",
    secondary: "Ver opções",
    trust: ["Produção local", "Encomendas particulares", "Fornecimento profissional"],
    productsKicker: "ESCOLHA O SEU FORMATO",
    productsTitle: "Um salgado para cada ocasião",
    cards: [
      ["Festas & família", "Combinações para aniversários, encontros e fins de semana.", "Pedir opções"],
      ["Congelados", "Prontos para ter em casa e preparar quando quiser.", "Consultar catálogo"],
      ["Cafés & lojas", "Fornecimento regular adaptado ao ritmo do seu negócio.", "Falar connosco"],
    ],
    b2bKicker: "PARA PROFISSIONAIS",
    b2bTitle: "O seu balcão, sempre bem servido.",
    b2bText: "Soluções para cafés, pastelarias, mercearias, hotéis, associações e eventos. Peça condições, formatos e uma proposta ajustada.",
    steps: [["01", "Conte-nos o que precisa"], ["02", "Receba uma proposta"], ["03", "Combine entrega ou recolha"]],
    order: "Fazer uma encomenda",
    orderText: "Diga-nos a ocasião, a quantidade aproximada e a data. Respondemos com as opções disponíveis.",
    name: "Nome",
    contact: "Telefone ou e-mail",
    need: "O que procura?",
    message: "Ex.: 100 salgados para uma festa no sábado",
    submit: "Enviar pedido",
    note: "Sem compromisso. Preços e disponibilidade sob consulta.",
    address: "Rua 5 de Outubro, Galeria Centro, Loja 8 · Fundão",
    footer: "Sabor do Fundão, feito para chegar mais longe.",
    alert: "Formulário demonstrativo. O contacto será ligado quando a empresa confirmar o destino das mensagens.",
  },
  en: {
    nav: ["Products", "For business", "Order"],
    eyebrow: "Local flavour · Made in Fundão",
    titleA: "Savoury bites made",
    titleB: "to be shared.",
    intro: "Portuguese flavour for your table, celebration or business. Local production, easy ordering and delivery by arrangement.",
    cta: "Request catalogue",
    secondary: "See options",
    trust: ["Local production", "Private orders", "Professional supply"],
    productsKicker: "CHOOSE YOUR FORMAT",
    productsTitle: "A savoury bite for every occasion",
    cards: [
      ["Parties & family", "Selections for birthdays, gatherings and weekends.", "Ask for options"],
      ["Frozen", "Ready to keep at home and prepare whenever you like.", "View catalogue"],
      ["Cafés & shops", "Regular supply tailored to the pace of your business.", "Talk to us"],
    ],
    b2bKicker: "FOR PROFESSIONALS",
    b2bTitle: "Keep your counter ready.",
    b2bText: "Solutions for cafés, bakeries, grocery shops, hotels, associations and events. Ask for terms, formats and a tailored proposal.",
    steps: [["01", "Tell us what you need"], ["02", "Receive a proposal"], ["03", "Arrange delivery or pickup"]],
    order: "Place an order",
    orderText: "Tell us the occasion, approximate quantity and date. We will reply with available options.",
    name: "Name",
    contact: "Phone or email",
    need: "What do you need?",
    message: "E.g. 100 savoury bites for a Saturday party",
    submit: "Send request",
    note: "No obligation. Prices and availability on request.",
    address: "Rua 5 de Outubro, Galeria Centro, Loja 8 · Fundão",
    footer: "The taste of Fundão, made to travel further.",
    alert: "Demo form. The contact destination will be connected once confirmed by the company.",
  },
  ua: {
    nav: ["Продукція", "Для бізнесу", "Замовити"],
    eyebrow: "Місцевий смак · Вироблено у Фундані",
    titleA: "Закуски, створені",
    titleB: "щоб ділитися.",
    intro: "Португальський смак для вашого столу, свята чи бізнесу. Місцеве виробництво, просте замовлення та доставка за домовленістю.",
    cta: "Запросити каталог",
    secondary: "Переглянути варіанти",
    trust: ["Місцеве виробництво", "Приватні замовлення", "Постачання для бізнесу"],
    productsKicker: "ОБЕРІТЬ СВІЙ ФОРМАТ",
    productsTitle: "Закуски для кожної нагоди",
    cards: [
      ["Свята та родина", "Набори для днів народження, зустрічей і вихідних.", "Дізнатися варіанти"],
      ["Заморожені", "Зручно зберігати вдома та готувати коли потрібно.", "Переглянути каталог"],
      ["Кафе та магазини", "Регулярне постачання під ритм вашого бізнесу.", "Зв’язатися"],
    ],
    b2bKicker: "ДЛЯ ПРОФЕСІОНАЛІВ",
    b2bTitle: "Ваша вітрина завжди готова.",
    b2bText: "Рішення для кафе, кондитерських, магазинів, готелів, асоціацій та подій. Запитайте умови, формати та індивідуальну пропозицію.",
    steps: [["01", "Розкажіть, що потрібно"], ["02", "Отримайте пропозицію"], ["03", "Домовтеся про доставку чи самовивіз"]],
    order: "Зробити замовлення",
    orderText: "Вкажіть подію, приблизну кількість і дату. Ми відповімо доступними варіантами.",
    name: "Ім’я",
    contact: "Телефон або e-mail",
    need: "Що вам потрібно?",
    message: "Напр.: 100 закусок на свято в суботу",
    submit: "Надіслати запит",
    note: "Без зобов’язань. Ціни та наявність — за запитом.",
    address: "Rua 5 de Outubro, Galeria Centro, Loja 8 · Fundão",
    footer: "Смак Фундана, створений, щоб подорожувати далі.",
    alert: "Демонстраційна форма. Контакт буде підключено після підтвердження компанією.",
  },
};

export default function Home() {
  const [lang, setLang] = useState<keyof typeof copy>("pt");
  const t = copy[lang];

  return (
    <main>
      <section className="hero">
        <header>
          <a className="brand" href="#"><span>S</span><div>Salgados<br/><b>do Fundão</b></div></a>
          <nav>{t.nav.map((x, i) => <a key={x} href={["#produtos", "#negocios", "#encomendar"][i]}>{x}</a>)}</nav>
          <div className="languages">{(["pt", "en", "ua"] as const).map(x => <button className={lang === x ? "active" : ""} onClick={() => setLang(x)} key={x}>{x.toUpperCase()}</button>)}</div>
        </header>
        <div className="orbit orbit1" /><div className="orbit orbit2" />
        <div className="crumb crumb1">●</div><div className="crumb crumb2">●</div><div className="crumb crumb3">●</div>
        <div className="heroContent">
          <p className="eyebrow"><i />{t.eyebrow}</p>
          <h1>{t.titleA}<br/><em>{t.titleB}</em></h1>
          <p className="intro">{t.intro}</p>
          <div className="actions"><a className="primary" href="#encomendar">{t.cta} <span>→</span></a><a className="ghost" href="#produtos">{t.secondary}</a></div>
        </div>
        <div className="plate" aria-hidden="true">
          <div className="food f1" /><div className="food f2" /><div className="food f3" /><div className="food f4" /><div className="food f5" />
          <div className="leaf l1">◆</div><div className="leaf l2">◆</div>
        </div>
        <div className="trust">{t.trust.map(x => <span key={x}>✓ {x}</span>)}</div>
        <div className="marquee"><div>COXINHAS · RISSÓIS · CROQUETES · CHAMUÇAS · SABOR LOCAL · COXINHAS · RISSÓIS · CROQUETES · CHAMUÇAS · SABOR LOCAL ·</div></div>
      </section>

      <section className="products" id="produtos">
        <p className="sectionKicker">{t.productsKicker}</p>
        <h2>{t.productsTitle}</h2>
        <div className="cards">{t.cards.map((card, i) => <article key={card[0]} className={`card card${i + 1}`}><div className="cardVisual"><span>{["✦","◉","▰"][i]}</span></div><div className="cardBody"><small>0{i+1}</small><h3>{card[0]}</h3><p>{card[1]}</p><a href="#encomendar">{card[2]} →</a></div></article>)}</div>
      </section>

      <section className="business" id="negocios">
        <div><p className="sectionKicker">{t.b2bKicker}</p><h2>{t.b2bTitle}</h2><p>{t.b2bText}</p></div>
        <div className="steps">{t.steps.map(s => <div key={s[0]}><b>{s[0]}</b><span>{s[1]}</span></div>)}</div>
      </section>

      <section className="order" id="encomendar">
        <div><p className="sectionKicker">ENCOMENDAS</p><h2>{t.order}</h2><p>{t.orderText}</p><div className="contactLine">☎ 275 772 642</div><div className="address">{t.address}</div></div>
        <form onSubmit={(e) => {e.preventDefault(); alert(t.alert);}}>
          <label>{t.name}<input required /></label>
          <label>{t.contact}<input required /></label>
          <label>{t.need}<textarea placeholder={t.message} required /></label>
          <button>{t.submit} <span>→</span></button>
          <small>{t.note}</small>
        </form>
      </section>
      <footer><a className="brand" href="#"><span>S</span><div>Salgados<br/><b>do Fundão</b></div></a><p>{t.footer}</p><p>© 2026</p></footer>
    </main>
  );
}
