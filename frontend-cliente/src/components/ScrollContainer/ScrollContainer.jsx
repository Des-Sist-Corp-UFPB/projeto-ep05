import { useRef, useState } from "react";
import './ScrollContainer.css';

const ScrollContainer = ({ children }) => {
  const scrollRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [startX, setStartX] = useState(0);
  const [scrollLeft, setScrollLeft] = useState(0);

  const startDragging = (e) => {
    setIsDragging(true);
    // e.pageX pega a posição horizontal do mouse
    setStartX(e.pageX - scrollRef.current.offsetLeft);
    setScrollLeft(scrollRef.current.scrollLeft);
  };

  const stopDragging = () => {
    setIsDragging(false);
  };

  const onMouseMove = (e) => {
    if (!isDragging) return;
    e.preventDefault(); // Impede seleções de texto indesejadas
    
    const x = e.pageX - scrollRef.current.offsetLeft;
    // O multiplicador (ex: 2) define a velocidade do arraste
    const walk = (x - startX) * 2; 
    scrollRef.current.scrollLeft = scrollLeft - walk;
  };

  return (
    <div 
      className={`scroll-wrapper ${isDragging ? "active" : ""}`}
      ref={scrollRef}
      onMouseDown={startDragging}
      onMouseLeave={stopDragging}
      onMouseUp={stopDragging}
      onMouseMove={onMouseMove}
    >
      {children}
    </div>
  );
};

export default ScrollContainer;