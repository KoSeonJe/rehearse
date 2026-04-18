# Frontend Design Rules — Prevent AI Slop

You tend to converge on "AI slop" aesthetics when working on frontend tasks.
Follow these rules strictly. Exceptions only when the user explicitly permits.

---

## 🚫 Colors — Strictly Forbidden

- **Do NOT use purple (#6366f1, indigo-500, violet, purple) family as primary**
    - Forbidden gradients: `from-purple-* to-blue-*`, `from-indigo-* to-pink-*`
    - Forbidden combination: "purple on white background"
    - Tailwind default palette indigo/violet/purple/fuchsia families only if user explicitly specifies
- **Forbidden: neon purple accent + dark background combo** (the hallmark of SaaS landing AI slop)
- Use dominant 1 + accent 1-2 decisively. A "timid palette" where all colors are evenly distributed at similar saturation is forbidden
- Do NOT use purple/green gradient badges to indicate AI features
- **Do NOT use pure black (#000) + pure white (#fff) in dark mode** — hurts the eyes and signals amateur. Recommended range: `#0a0a0a` ~ `#f5f5f5`
- **Do NOT apply gradient + glow shadow to every button** — reserve emphasis for the primary CTA only, establish hierarchy

## 🚫 Typography — Strictly Forbidden

- **Do NOT use Inter, Roboto, Arial, Open Sans, Lato, or default system fonts**
- Space Grotesk is also a choice AI over-converges on — do NOT use as default
- Instead, choose based on context:
    - Editorial: Fraunces, Playfair Display, Crimson Pro
    - Tech/code: JetBrains Mono, IBM Plex, Fira Code
    - Characterful sans-serif: Clash Display, Satoshi, Bricolage Grotesque
    - For Korean projects: consider Pretendard, Noto Sans KR, but escape monotony with a distinctive display font
- Create contrast by pairing Display font + Body font
- **Do NOT sprinkle emoji icons (🚀 ✨ 💡 🎯) in section titles** — breaks immediately, especially in Korean service tones

## 🚫 Layout / Visual Elements — Strictly Forbidden

- **Do NOT use glassmorphism (backdrop-blur + translucent cards) as the default style**
    - Use intentionally, on a single element that truly needs it
- Do NOT uniformly apply `rounded-lg`, `rounded-xl` (8px/16px) to all cards. Differentiate radius intentionally per element
- **Do NOT build feature sections with a 3-column icon grid** (the most AI-looking layout)
- Do NOT unify all icons as Lucide/Heroicons outline style. Mix filled/duotone/custom SVG when appropriate
- Avoid the predictable landing page structure: hero → 3-column features → social proof → CTA → footer
- Do NOT generate faceless 3D character illustrations (glowing orbs, floating UI elements)
- **Do NOT apply `transition-all duration-300` uniformly to everything** — intentionless blanket animation destroys hierarchy. Specify only necessary properties (`transition-colors`, `transition-transform`, etc.)

## 🚫 Content / Copy — Strictly Forbidden

- **Forbidden: ungrounded social proof like "Built with ❤️", "Loved by 10,000+ developers"**
- **Forbidden: leftover scaffolding like Lorem ipsum, "Product Name", "Lorem Company"** — replace with real content or contextual dummy text
- **Forbidden: English default placeholders like "Enter your email..."** — match the project's language (Korean project → "이메일을 입력해주세요" etc.)
- Watch for English leak everywhere: in Korean projects, error messages, button labels, and aria-labels must all be in Korean
- Forbidden AI cliché phrases: "in today's fast-paced world", "in the ever-evolving landscape", "unlock", "delve into", etc.

---

## 🎨 Brand Point Color — Teal (2026-04-18 확정)

- **Brand 시그니처**: `--brand` (`#0F766E` light / `#2dd4bf` dark). Tailwind: `bg-brand`, `text-brand`, `ring-brand`, `hover:bg-brand-hover`, `bg-brand-bg`
- **사용해야 할 곳**: Primary CTA 버튼, 인라인 링크, focus ring, active 네비, 선택된 step/pill, 타임라인 scrubber, 챕터 배지(진행 맥락)
- **사용하면 안 되는 곳**:
  - Signal 계열(record/warning/success)과 **같은 요소에 동시 배치 금지** — `bg-brand`와 `text-signal-warning`을 한 버튼에 섞지 말 것
  - Feedback 페이지 **읽기 맥락의 editorial 장식**(인용문 border, 챕터 숫자 over-line)에는 `accent-editorial` 유지. teal로 바꾸면 "읽기 톤"이 "액션 톤"으로 변질됨
  - Signal-success (sage green)과 동일 화면에 대형으로 공존 금지 — 색약 시뮬레이션(Deuteranopia)에서 구분 불가

---

## ✅ What To Do Instead

- Before starting work, commit to one aesthetic direction: brutalist, editorial, retro-futuristic, soft pastel, industrial, playful, luxury, maximalist, refined minimal, etc.
- Tokenize colors/spacing/radius with CSS variables — do NOT hardcode values
- Create atmosphere in backgrounds: layer gradient meshes, noise textures, geometric patterns, grain overlays to match the aesthetic
- Rather than scattering animations, execute one staggered reveal properly
- Allow unexpected layouts: asymmetry, overlap, diagonal flow, grid-breaking
- Maintain WCAG contrast ratio of 4.5:1 or higher — especially for text over gradients

---

## 🔍 Self-Check After Work

Before committing code, ask yourself these questions:

1. Is the primary color purple/indigo? → Is there a reason?
2. Is the font Inter/Roboto? → Replace immediately
3. Is there a 3-column icon grid below the hero? → Reconsider the structure
4. Does every card have backdrop-blur? → Keep only one
5. Am I using pure black/white in dark mode? → Replace with off-black/off-white
6. Am I overusing `transition-all`? → Use only necessary properties
7. Is Lorem ipsum or English placeholder still present? → Replace with actual context
8. Could this screen be used as another SaaS landing by swapping just the company name? → If yes, add context-specific elements
9. Can I name "the one memorable thing" about this UI? → If not, create a differentiator
