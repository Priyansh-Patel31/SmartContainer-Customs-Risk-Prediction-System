import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Send, User as UserIcon, MessageSquare, Loader2, ArrowLeft } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';
import { cn } from '@/lib/utils';
import { fetchChatConversations, fetchChatMessages, sendChatMessage } from '@/api/routes';

// Use polling for real-time feel
const POLLING_INTERVAL = 5000;

export default function Chat() {
    const { user } = useAuth();
    const queryClient = useQueryClient();
    const isExporter = user?.role === 'exporter';

    const [selectedConvoId, setSelectedConvoId] = useState<string | null>(null);
    const [messageInput, setMessageInput] = useState('');
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Fetch conversations list
    const { data: convosData, isLoading: isLoadingConvos } = useQuery({
        queryKey: ['chat-conversations'],
        queryFn: () => fetchChatConversations(),
        refetchInterval: POLLING_INTERVAL,
    });

    const conversations = convosData?.data || [];

    // Fetch messages for selected conversation
    const { data: messagesData, isLoading: isLoadingMessages } = useQuery({
        queryKey: ['chat-messages', selectedConvoId],
        queryFn: () => selectedConvoId ? fetchChatMessages(selectedConvoId) : Promise.resolve(null),
        enabled: !!selectedConvoId,
        refetchInterval: POLLING_INTERVAL,
    });

    const messages = messagesData?.data || [];
    const activeConvo = conversations.find(c => c.conversation_id === selectedConvoId);

    // Auto-scroll to bottom when messages load
    useEffect(() => {
        if (messagesEndRef.current) {
            messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }, [messages]);

    // Send message mutation
    const sendMutation = useMutation({
        mutationFn: (text: string) => sendChatMessage({ conversation_id: selectedConvoId!, message_text: text }),
        onSuccess: () => {
            setMessageInput('');
            queryClient.invalidateQueries({ queryKey: ['chat-messages', selectedConvoId] });
            queryClient.invalidateQueries({ queryKey: ['chat-conversations'] });
        }
    });

    const handleSend = (e: React.FormEvent) => {
        e.preventDefault();
        if (!messageInput.trim() || !selectedConvoId) return;
        sendMutation.mutate(messageInput.trim());
    };

    return (
        <div className="flex h-full bg-card rounded-xl border border-border shadow-sm overflow-hidden min-h-[600px] max-h-[800px]">
            {/* Left Pane - List */}
            <div className={cn(
                "w-full md:w-80 flex-col border-r border-border bg-sidebar/50",
                selectedConvoId ? "hidden md:flex" : "flex"
            )}>
                <div className="p-4 border-b border-border bg-card flex items-center gap-2">
                    <MessageSquare className="w-5 h-5 text-primary" />
                    <h2 className="font-semibold text-foreground">Conversations</h2>
                </div>
                
                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                    {isLoadingConvos ? (
                        <div className="flex justify-center p-4"><Loader2 className="w-5 h-5 animate-spin text-foreground/40" /></div>
                    ) : conversations.length === 0 ? (
                        <div className="text-center p-8 text-foreground/40 text-sm">
                            No active conversations.<br/>
                            {!isExporter && "Select a container to start chatting."}
                        </div>
                    ) : (
                        conversations.map(convo => (
                            <button
                                key={convo.conversation_id}
                                onClick={() => setSelectedConvoId(convo.conversation_id)}
                                className={cn(
                                    "w-full text-left p-3 rounded-lg flex items-start gap-3 transition-colors",
                                    selectedConvoId === convo.conversation_id 
                                        ? "bg-primary/10 border border-primary/20" 
                                        : "hover:bg-foreground/5"
                                )}
                            >
                                <div className="w-10 h-10 rounded-full bg-foreground/10 flex items-center justify-center shrink-0">
                                    <UserIcon className="w-5 h-5 text-foreground/60" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex justify-between items-center mb-1">
                                        <h4 className="font-medium text-sm truncate">
                                            {isExporter ? 'Admin Support' : convo.participants?.exporter || 'Exporter'}
                                        </h4>
                                        <span className="text-[10px] text-foreground/40">
                                            {convo.last_message?.timestamp ? new Date(convo.last_message.timestamp).toLocaleDateString() : ''}
                                        </span>
                                    </div>
                                    <p className="text-xs text-foreground/60 truncate">
                                        Container: {convo.container_id}
                                    </p>
                                    <p className="text-xs text-foreground/50 truncate mt-0.5">
                                        {convo.last_message?.preview || "No messages yet"}
                                    </p>
                                </div>
                            </button>
                        ))
                    )}
                </div>
            </div>

            {/* Right Pane - Chat Window */}
            <div className={cn(
                "flex-1 flex-col bg-background/50",
                selectedConvoId ? "flex" : "hidden md:flex"
            )}>
                {selectedConvoId && activeConvo ? (
                    <>
                        {/* Chat Header */}
                        <div className="p-4 border-b border-border bg-card flex items-center justify-between shadow-sm z-10">
                            <div className="flex items-center gap-3">
                                <button 
                                    onClick={() => setSelectedConvoId(null)}
                                    className="md:hidden p-1 hover:bg-foreground/10 rounded-lg text-foreground/60"
                                >
                                    <ArrowLeft className="w-5 h-5" />
                                </button>
                                <div>
                                    <h3 className="font-semibold flex items-center gap-2">
                                        {isExporter ? 'Admin Support' : activeConvo.participants?.exporter || 'Exporter'}
                                    </h3>
                                    <p className="text-xs text-foreground/50 font-mono">
                                        Container: {activeConvo.container_id} 
                                        {activeConvo.risk_level && ` • Risk: ${activeConvo.risk_level}`}
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Messages Area */}
                        <div className="flex-1 overflow-y-auto p-4 space-y-4">
                            {isLoadingMessages ? (
                                <div className="flex justify-center flex-1 items-center"><Loader2 className="w-6 h-6 animate-spin text-primary/50" /></div>
                            ) : messages.length === 0 ? (
                                <div className="h-full flex flex-col items-center justify-center text-foreground/40 space-y-2">
                                    <MessageSquare className="w-8 h-8 opacity-20" />
                                    <p className="text-sm">No messages yet. Start the conversation!</p>
                                </div>
                            ) : (
                                messages.map((msg) => {
                                    const isSystem = msg.sender_role === 'system';
                                    const isMe = String(msg.sender_id) === String(user?._id) || (msg.sender_role === user?.role);

                                    if (isSystem) {
                                        return (
                                            <div key={msg.message_id} className="flex justify-center my-4">
                                                <div className="bg-amber-500/10 text-amber-500 border border-amber-500/20 text-xs px-3 py-1.5 rounded-full max-w-[80%] text-center">
                                                    {msg.message_text}
                                                </div>
                                            </div>
                                        );
                                    }

                                    return (
                                        <div key={msg.message_id} className={cn("flex flex-col gap-1 max-w-[75%]", isMe ? "ml-auto items-end" : "mr-auto items-start")}>
                                            <div className="flex items-baseline gap-2">
                                                <span className="text-[10px] text-foreground/40 font-medium">
                                                    {isMe ? 'You' : msg.sender_name}
                                                </span>
                                                <span className="text-[9px] text-foreground/30">
                                                    {new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                                </span>
                                            </div>
                                            <div className={cn(
                                                "px-4 py-2.5 rounded-2xl text-sm leading-relaxed shadow-sm",
                                                isMe 
                                                    ? "bg-primary text-primary-foreground rounded-br-sm" 
                                                    : "bg-card border border-border text-card-foreground rounded-bl-sm"
                                            )}>
                                                {msg.message_text}
                                            </div>
                                        </div>
                                    );
                                })
                            )}
                            <div ref={messagesEndRef} />
                        </div>

                        {/* Input Area */}
                        <div className="p-4 bg-card border-t border-border">
                            <form onSubmit={handleSend} className="flex items-end gap-2">
                                <textarea 
                                    value={messageInput}
                                    onChange={(e) => setMessageInput(e.target.value)}
                                    placeholder="Type your message..."
                                    className="flex-1 bg-background border border-border rounded-xl px-4 py-3 text-sm focus:outline-none focus:ring-2 focus:ring-primary/50 text-foreground resize-none"
                                    rows={2}
                                    onKeyDown={(e) => {
                                        if (e.key === 'Enter' && !e.shiftKey) {
                                            e.preventDefault();
                                            handleSend(e);
                                        }
                                    }}
                                />
                                <button 
                                    type="submit"
                                    disabled={!messageInput.trim() || sendMutation.isPending}
                                    className="p-3 bg-primary text-primary-foreground rounded-xl shadow-sm hover:opacity-90 disabled:opacity-50 transition-opacity"
                                >
                                    {sendMutation.isPending ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
                                </button>
                            </form>
                        </div>
                    </>
                ) : (
                    <div className="h-full flex flex-col items-center justify-center text-foreground/40">
                        <MessageSquare className="w-12 h-12 mb-4 opacity-20" />
                        <h3 className="text-lg font-medium text-foreground/60 mb-1">Your Messages</h3>
                        <p className="text-sm">Select a conversation from the sidebar to start chatting</p>
                    </div>
                )}
            </div>
        </div>
    );
}
