import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import Button from '../components/Button/Button';
import Input from '../components/Input/Input';

// ── Button ────────────────────────────────────────────────────────────────────

describe('Button', () => {
  it('deve renderizar o texto filho', () => {
    render(<Button>Confirmar</Button>);
    expect(screen.getByText('Confirmar')).toBeInTheDocument();
  });

  it('variant padrão deve ser "primary"', () => {
    render(<Button>Ação</Button>);
    expect(screen.getByRole('button')).toHaveClass('btn--primary');
  });

  it('deve aplicar variant recebida via prop', () => {
    render(<Button variant="secondary">Cancelar</Button>);
    expect(screen.getByRole('button')).toHaveClass('btn--secondary');
  });

  it('deve chamar onClick ao clicar', () => {
    const onClick = vi.fn();
    render(<Button onClick={onClick}>Clique</Button>);
    fireEvent.click(screen.getByRole('button'));
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('disabled deve desabilitar o botão', () => {
    render(<Button disabled>Bloqueado</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('não deve chamar onClick quando desabilitado', () => {
    const onClick = vi.fn();
    render(<Button disabled onClick={onClick}>Bloqueado</Button>);
    fireEvent.click(screen.getByRole('button'));
    expect(onClick).not.toHaveBeenCalled();
  });

  it('deve renderizar ícone quando fornecido', () => {
    render(<Button icon={<span data-testid="icone">★</span>}>Salvar</Button>);
    expect(screen.getByTestId('icone')).toBeInTheDocument();
  });

  it('type padrão deve ser "button"', () => {
    render(<Button>Botão</Button>);
    expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
  });

  it('deve aceitar type="submit"', () => {
    render(<Button type="submit">Enviar</Button>);
    expect(screen.getByRole('button')).toHaveAttribute('type', 'submit');
  });

  it('deve aplicar className adicional', () => {
    render(<Button className="minha-classe">Botão</Button>);
    expect(screen.getByRole('button')).toHaveClass('minha-classe');
  });
});

// ── Input ─────────────────────────────────────────────────────────────────────

describe('Input', () => {
  it('deve renderizar o campo de entrada', () => {
    render(<Input type="text" placeholder="Digite algo" />);
    expect(screen.getByPlaceholderText('Digite algo')).toBeInTheDocument();
  });

  it('deve renderizar label quando fornecida', () => {
    render(<Input label="E-mail" id="email" />);
    expect(screen.getByLabelText('E-mail')).toBeInTheDocument();
  });

  it('não deve renderizar label quando não fornecida', () => {
    render(<Input type="text" />);
    expect(screen.queryByRole('label')).not.toBeInTheDocument();
  });

  it('deve exibir mensagem de erro quando erro é fornecido', () => {
    render(<Input erro="Campo obrigatório" />);
    expect(screen.getByText('Campo obrigatório')).toBeInTheDocument();
  });

  it('deve aplicar classe input-error quando há erro', () => {
    render(<Input erro="Inválido" />);
    const input = screen.getByRole('textbox');
    expect(input.className).toMatch(/input-error/);
  });

  it('não deve ter classe input-error quando não há erro', () => {
    render(<Input type="text" />);
    const input = screen.getByRole('textbox');
    expect(input.className).not.toMatch(/input-error/);
  });

  it('deve passar props adicionais para o input', () => {
    render(<Input type="email" name="email" data-testid="email-input" />);
    expect(screen.getByTestId('email-input')).toHaveAttribute('type', 'email');
  });

  it('deve aplicar classe input-readonly quando readOnly', () => {
    render(<Input readOnly />);
    expect(screen.getByRole('textbox').className).toMatch(/input-readonly/);
  });
});
